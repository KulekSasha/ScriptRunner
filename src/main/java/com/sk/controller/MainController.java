package com.sk.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.sk.executor.UserScriptExecutor;
import com.sk.model.UserScript;

@RestController
public class MainController {

    @Resource
    private UserScriptExecutor scriptExecutor;

    @RequestMapping(value = "/scripts", method = RequestMethod.POST)
    public ResponseEntity<Void> addNewScript(@RequestBody UserScript script, UriComponentsBuilder ucBuilder) {
        long newId = scriptExecutor.add(script);
        // TODO Error handling
        // What if script has syntax errors? I'd recommend to check for errors synchronously and return error immediately. Consider compiling script before scheduling
        return ResponseEntity.created(ucBuilder.path("/scripts/{id}").buildAndExpand(newId).toUri()).build();
    }


    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getScriptById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getById(id);
        if (uScript == null) {
            return  ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(uScript);
    }

    @RequestMapping(value = "/scripts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserScript>> getAllScripts() {
        List<UserScript> uScripts = scriptExecutor.getAll();
        if (uScripts.isEmpty()) {
        	// TODO use builder pattern, see above
        	// result should be OK with empty array
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(uScripts, HttpStatus.OK);
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<UserScript> deleteScript(@PathVariable("id") long id) {
        if (scriptExecutor.deleteById(id)) {
        	// TODO OK response code should return valid body. Without body code should be NO_CONTENT
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


}
