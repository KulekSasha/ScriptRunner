package com.sk.model;

import java.time.LocalDateTime;

public class UserScript {

    private long id;
    private String script;
    private volatile String result;
    private volatile ScriptStatus status;
    private volatile LocalDateTime lastStatusChange;

    public UserScript() {
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

    public synchronized ScriptStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(ScriptStatus status) {
        this.status = status;
        changeTimestamp();
    }

    void changeTimestamp() {
    	this.setLastStatusChange(LocalDateTime.now());
	}

	public synchronized LocalDateTime getLastStatusChange() {
        return lastStatusChange;
    }

    void setLastStatusChange(LocalDateTime lastStatusChange) {
        this.lastStatusChange = lastStatusChange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserScript)) return false;

        UserScript that = (UserScript) o;

        if (id != that.id) return false;
        return script != null ? script.equals(that.script) : that.script == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (script != null ? script.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserScript{" +
                "id=" + id +
                ", script='" + script + '\'' +
                ", result='" + result + '\'' +
                ", status=" + status +
                ", lastStatusChange=" + lastStatusChange +
                '}';
    }
}
