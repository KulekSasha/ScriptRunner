package com.sk.controller;

import com.sk.executor.UserScriptExecutor;
import com.sk.model.UserScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class MainController {

    @Resource(name = "scriptExecutor")
    private UserScriptExecutor scriptExecutor;

    @RequestMapping(value = "/scripts", method = RequestMethod.POST)
    public ResponseEntity<Void> addNewScript(@RequestBody UserScript script, UriComponentsBuilder ucBuilder) {
        long newId = scriptExecutor.add(script);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/scripts/{id}").buildAndExpand(newId).toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);

    }


    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserScript> getScriptById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getById(id);
        if (uScript == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(uScript, HttpStatus.OK);
    }

    @RequestMapping(value = "/scripts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserScript>> getAllScripts() {
        List<UserScript> uScripts = scriptExecutor.getAll();
        if (uScripts.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(uScripts, HttpStatus.OK);
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<UserScript> deleteScript(@PathVariable("id") long id) {
        if (scriptExecutor.deleteById(id)) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


}
