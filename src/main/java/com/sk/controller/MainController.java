package com.sk.controller;

import com.sk.model.UserScript;
import com.sk.service.ScriptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class MainController {

    @Resource(name = "scriptService")
    private ScriptService scriptService;

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<UserScript> test(@RequestBody UserScript input) {
        return new ResponseEntity<>(scriptService.eval(input), HttpStatus.ACCEPTED);
    }

}
