package com.sk.executor;

import com.sk.model.ScriptStatus;
import com.sk.model.UserScript;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class UserScriptExecutorImpl implements UserScriptExecutor {

    private AtomicLong counter = new AtomicLong(1);
    private ConcurrentMap<Long, Holder> scriptStorage = new ConcurrentHashMap<>();
    //    private ConcurrentMap<Long, UserScript> scriptStorage = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private ScriptEngineManager manager = new ScriptEngineManager();

    public UserScriptExecutorImpl(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }


    @Override
    public synchronized long add(UserScript script) {
        script.setId(counter.getAndIncrement());
        script.setStatus(ScriptStatus.WAITING);
        script.setLastStatusChange(LocalDateTime.now());


        Future task = executor.submit(() -> {
            ScriptEngine engine = manager.getEngineByName("nashorn");

            ScriptResultWriter writer = new ScriptResultWriter();
            engine.getContext().setWriter(writer);
            engine.getContext().setErrorWriter(writer);

            script.setStatus(ScriptStatus.IN_PROGRESS);
            script.setLastStatusChange(LocalDateTime.now());

            try {
                engine.eval(script.getScript());
            } catch (final ScriptException se) {
                System.out.println(se);
                script.setResult(se.toString());
                script.setStatus(ScriptStatus.COMPLETE_WITH_ERROR);
                script.setLastStatusChange(LocalDateTime.now());
                return;
            }

            script.setResult(writer.getStringWriter().getBuffer().toString());
            script.setStatus(ScriptStatus.COMPLETE);
            script.setLastStatusChange(LocalDateTime.now());
        });

        scriptStorage.put(script.getId(), new Holder(script, task));

        return script.getId();
    }

    @Override
    public List<UserScript> getAll() {
        return scriptStorage.entrySet().stream()
                .map(e -> e.getValue().getUserScript())
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


        public StringWriter getStringWriter() {
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

    private class Holder {
        private UserScript userScript;
        private Future future;

        public Holder(UserScript userScript, Future future) {
            this.userScript = userScript;
            this.future = future;
        }

        public UserScript getUserScript() {
            return userScript;
        }

        public void setUserScript(UserScript userScript) {
            this.userScript = userScript;
        }

        public Future getFuture() {
            return future;
        }

        public void setFuture(Future future) {
            this.future = future;
        }
    }
}


