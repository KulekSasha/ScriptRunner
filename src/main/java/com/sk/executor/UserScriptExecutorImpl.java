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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class UserScriptExecutorImpl implements UserScriptExecutor {

    //    private static int POOL_SIZE;
    private AtomicLong counter = new AtomicLong(1);
    //    private ConcurrentMap<Long, Holder> scriptStorage = new ConcurrentHashMap<>();
    private ConcurrentMap<Long, UserScript> scriptStorage = new ConcurrentHashMap<>();
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

        scriptStorage.put(script.getId(), script);

        executor.submit(() -> {
            ScriptEngine engine = manager.getEngineByName("nashorn");

            ScriptResultWriter writer = new ScriptResultWriter();
            engine.getContext().setWriter(writer);

            script.setStatus(ScriptStatus.IN_PROGRESS);
            script.setLastStatusChange(LocalDateTime.now());

            try {
                engine.eval(script.getScript());
            } catch (final ScriptException se) {
                script.setResult("ERROR: " + se.toString());
                script.setStatus(ScriptStatus.COMPLETE_WITH_ERROR);
                script.setLastStatusChange(LocalDateTime.now());
                return;
            }

            script.setResult(writer.getStringWriter().getBuffer().toString());
            script.setStatus(ScriptStatus.COMPLETE);
            script.setLastStatusChange(LocalDateTime.now());
        });

        return script.getId();
    }

    @Override
    public List<UserScript> getAll() {
        return scriptStorage.entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public UserScript getById(long id) {
        return scriptStorage.get(id);
    }

    @Override
    public boolean deleteById(long id) {
        return scriptStorage.remove(id) != null;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private class ScriptResultWriter extends Writer {

        StringWriter strWriter = new StringWriter();
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

}


//class Holder {
//    private UserScript userScript;
//    private Future<String> future;
//
//    public Holder(UserScript userScript, Future<String> future) {
//        this.userScript = userScript;
//        this.future = future;
//    }
//
//    public UserScript getUserScript() {
//        return userScript;
//    }
//
//    public void setUserScript(UserScript userScript) {
//        this.userScript = userScript;
//    }
//
//    public Future<String> getFuture() {
//        return future;
//    }
//
//    public void setFuture(Future<String> future) {
//        this.future = future;
//    }
//}