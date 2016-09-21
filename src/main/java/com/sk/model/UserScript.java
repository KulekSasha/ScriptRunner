package com.sk.model;

public class UserScript {

    private long id;
    private String script;
    private String result;

    public UserScript() {
    }

    public UserScript(long id, String script) {
        this.id = id;
        this.script = script;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
