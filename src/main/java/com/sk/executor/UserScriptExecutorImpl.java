package com.sk.executor;

import com.sk.model.ScriptStatus;
import com.sk.model.UserScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Compilable;
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
    private ExecutorService executor;
    private ScriptEngineManager manager = new ScriptEngineManager();
    private Compilable compiler = (Compilable) manager.getEngineByName("nashorn");

    private static final Logger LOG = LoggerFactory.getLogger(UserScriptExecutorImpl.class);
    private static final String VALIDATION_OK = "valid";


    public UserScriptExecutorImpl(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public synchronized long add(UserScript script) {
        script.setId(counter.getAndIncrement());
        script.setStatus(ScriptStatus.WAITING);
        LOG.debug("Script with id[{}] added. Script: [{}]", script.getId(), script.getScript());

        Holder holder = new Holder(script);

        Future<?> task = executor.submit(() -> {
            holder.setThread(Thread.currentThread());
            ScriptEngine engine = manager.getEngineByName("nashorn");

            ScriptResultWriter writer = new ScriptResultWriter();
            engine.getContext().setWriter(writer);
            engine.getContext().setErrorWriter(writer);

            holder.setWriter(writer);

            script.setStatus(ScriptStatus.IN_PROGRESS);
            script.setStartDateTime(LocalDateTime.now());
            try {
                engine.eval(script.getScript());
                // TODO DONE result should be updated during script execution, or on every request, not only at the end of execution,
                // otherwise it will be hard to understand what happens with buggy scripts.
                script.setResult(writer.getStringWriter().getBuffer().toString());
                script.setStatus(ScriptStatus.COMPLETE);
            } catch (final ScriptException se) {
                // TODO DONE use logging instead
                LOG.debug("Error during script eval.: ", se.toString());
                script.setResult(se.toString());
                script.setStatus(ScriptStatus.COMPLETE_WITH_ERROR);
            } catch (final Throwable e) {
                // TODO DONE what about handling other exceptions? Runtime exceptions ? Thread interruption? Errors?
                LOG.debug("Unexpected error/exception: {}", e.toString());
            }

        });

        holder.setTask(task);
        scriptStorage.put(script.getId(), holder);

        return script.getId();
    }

    @Override
    public List<UserScript> getAll() {
        return scriptStorage.values().stream()
                .map(h -> {
                    h.getUserScript().setResult(h.getWriter().getStringWriter().toString());
                    return h.getUserScript();
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserScript getById(long id) {
        Holder holder = scriptStorage.get(id);
        if (holder != null) {
            if (holder.getWriter() != null) {
                holder.getUserScript().setResult(holder.getWriter().getStringWriter().toString());
            }
        }

        return holder != null ? holder.getUserScript() : null;
    }

    @Override
    public UserScript getByIdWoDetails(long id) {
        Holder holder = scriptStorage.get(id);
        return holder != null ? holder.getUserScript() : null;
    }

    @Override
    public synchronized boolean deleteById(long id) {
        Holder holder = scriptStorage.remove(id);
        if (holder == null) {
            return false;
        }

        if (holder.getThread() != null) {
            try {
                holder.getThread().stop();
            } catch (SecurityException | IllegalMonitorStateException e) {
                LOG.debug("Error during thread stopping. Thread: {}. Error: {}", holder.getThread().getName(), e);
            }
        } else {
            holder.task.cancel(true);
        }
        return true;
    }

    @Override
    public String validate(String input) {
        try {
            compiler.compile(input);
            LOG.debug("Compiled successfully.");
        } catch (ScriptException e) {
            LOG.debug("Compiled with error: {}", e.toString());
            return e.toString();
        }
        return VALIDATION_OK;
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
        private volatile Future<?> task;
        private volatile Thread thread;
        private volatile ScriptResultWriter writer;


        Holder(UserScript userScript) {
            this.userScript = userScript;
        }

        void setTask(Future<?> task) {
            this.task = task;
        }

        void setThread(Thread thread) {
            this.thread = thread;
        }

        void setWriter(ScriptResultWriter writer) {
            this.writer = writer;
        }

        UserScript getUserScript() {
            return userScript;
        }

        Future<?> getTask() {
            return task;
        }

        Thread getThread() {
            return thread;
        }

        ScriptResultWriter getWriter() {
            return writer;
        }
    }
}


