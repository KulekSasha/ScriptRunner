package com.sk.executor;

import com.sk.model.UserScript;

import java.util.List;

public interface UserScriptExecutor {

    long add(UserScript script);

    List<UserScript> getAll();

    UserScript getById(long id);

    UserScript getByIdWoDetails(long id);

    boolean deleteById(long id);

    String validate(String input);
}
