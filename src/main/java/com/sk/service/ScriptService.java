package com.sk.service;

import com.sk.model.UserScript;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;

@Service
public class ScriptService {
    private ScriptEngineManager manager = new ScriptEngineManager();

    public UserScript eval(UserScript script) {
        ScriptEngine engine = manager.getEngineByName("nashorn");

        MyWriter writer = new MyWriter();
        engine.getContext().setWriter(writer);

        try {
            engine.eval(script.getScript());
        } catch (final ScriptException se) {
            script.setResult("ERROR: " + se.toString());
            return script;
        }

        script.setResult(writer.getStringWriter().getBuffer().toString());
        return script;
    }


    private class MyWriter extends Writer {

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
