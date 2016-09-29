package com.sk.executor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.sk.model.ScriptStatus;
import com.sk.model.UserScript;

public class UserScriptExecutorImpl implements UserScriptExecutor {

    private AtomicLong counter = new AtomicLong(1);
    private ConcurrentMap<Long, Holder> scriptStorage = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private ScriptEngineManager manager = new ScriptEngineManager();

    public UserScriptExecutorImpl(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }


    @Override
    public synchronized long add(UserScript script) {
        script.setId(counter.getAndIncrement());
        script.setStatus(ScriptStatus.WAITING);


        Future<?> task = executor.submit(() -> {
            ScriptEngine engine = manager.getEngineByName("nashorn");

            ScriptResultWriter writer = new ScriptResultWriter();
            engine.getContext().setWriter(writer);
            engine.getContext().setErrorWriter(writer);

            script.setStatus(ScriptStatus.IN_PROGRESS);

            try {
                engine.eval(script.getScript());
                // TODO result should be updated during script execution, or on every request, not only at the end of execution,
                // otherwise it will be hard to understand what happens with buggy scripts. 
                script.setResult(writer.getStringWriter().getBuffer().toString());
                script.setStatus(ScriptStatus.COMPLETE);
            } catch (final ScriptException se) {
            	// TODO use logging instead
                System.out.println(se.toString());
                script.setResult(se.toString());
                script.setStatus(ScriptStatus.COMPLETE_WITH_ERROR);
            }
            // TODO what about handling other exceptions? Runtime exceptions ? Thread interruption? Errors?

        });

        scriptStorage.put(script.getId(), new Holder(script, task));

        return script.getId();
    }

    @Override
    public List<UserScript> getAll() {
        return scriptStorage.values().stream()
                .map(e -> e.getUserScript())
                .collect(Collectors.toList());
    }

    @Override
    public UserScript getById(long id) {
        Holder holder = scriptStorage.get(id);
        return holder != null ? holder.getUserScript() : null;
    }

    @Override
    public synchronized boolean deleteById(long id) {
        Holder holder = scriptStorage.remove(id);
        if (holder == null) {
            return false;
        }
        holder.getFuture().cancel(true);
        return true;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private class ScriptResultWriter extends Writer {

        private StringWriter strWriter = new StringWriter();
        private PrintWriter stringWriter = new PrintWriter(strWriter, true);
        private PrintWriter consoleWriter = new PrintWriter(System.out, true);


        StringWriter getStringWriter() {
            return strWriter;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            stringWriter.write(cbuf, off, len);
            consoleWriter.write(cbuf, off, len);

        }

        @Override
        public void write(int c) throws IOException {
            stringWriter.write(c);
            consoleWriter.write(c);
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            stringWriter.write(cbuf);
            consoleWriter.write(cbuf);
        }

        @Override
        public void write(String str) throws IOException {
            stringWriter.write(str);
            consoleWriter.write(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            stringWriter.write(str, off, len);
            consoleWriter.write(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            stringWriter.append(csq);
            consoleWriter.append(csq);
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            stringWriter.append(csq, start, end);
            consoleWriter.append(csq, start, end);
            return this;
        }

        @Override
        public Writer append(char c) throws IOException {
            stringWriter.append(c);
            consoleWriter.append(c);
            return this;
        }

        @Override
        public void flush() throws IOException {
            stringWriter.flush();
            consoleWriter.flush();
        }

        @Override
        public void close() throws IOException {
            stringWriter.close();
            consoleWriter.close();
        }
    }

    private static class Holder {
        private final UserScript userScript;
        private final Future<?> future;

        Holder(UserScript userScript, Future<?> future) {
            this.userScript = userScript;
            this.future = future;
        }

        UserScript getUserScript() {
            return userScript;
        }

        Future<?> getFuture() {
            return future;
        }

    }
}


