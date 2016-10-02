package com.sk.controller;

import com.sk.executor.UserScriptExecutor;
import com.sk.model.UserScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.List;

import static org.springframework.http.ResponseEntity.notFound;

@RestController
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final String VALIDATION_OK = "valid";

    @Resource
    private UserScriptExecutor scriptExecutor;

    @RequestMapping(value = "/scripts", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> addNewScriptPlainText(@RequestBody String input, UriComponentsBuilder ucBuilder) {
        LOG.debug("Input: {{}}", input);
        String validationResult = scriptExecutor.validate(input);

        // TODO DONE Error handling
        // What if script has syntax errors? I'd recommend to check for errors synchronously and return error immediately. Consider compiling script before scheduling
        if (!validationResult.equals(VALIDATION_OK)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("CompilationError", validationResult)
                    .build();
        }

        UserScript userScript = new UserScript();
        userScript.setScript(input);
        long newId = scriptExecutor.add(userScript);

        return ResponseEntity.created(ucBuilder.path("/scripts/{id}").buildAndExpand(newId).toUri()).build();
    }


    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getScriptById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getByIdWoDetails(id);
        if (uScript == null) {
            return notFound().build();
        }
        return ResponseEntity.ok(uScript);
    }

    @RequestMapping(value = "/scripts/{id}/request", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> getScriptStringById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getById(id);
        if (uScript == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(uScript.getScript());
    }

    @RequestMapping(value = "/scripts/{id}/result", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> getScriptResultById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getById(id);
        if (uScript == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(uScript.getResult());
    }

    @RequestMapping(value = "/scripts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserScript>> getAllScripts() {
        List<UserScript> uScripts = scriptExecutor.getAll();
        // TODO DONE use builder pattern, see above
        return ResponseEntity.ok(uScripts);
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteScript(@PathVariable("id") long id) {
        if (scriptExecutor.deleteById(id)) {
            // TODO DONE - OK response code should return valid body. Without body code should be NO_CONTENT
            return ResponseEntity.noContent().build();
        } else {
            return notFound().build();
        }
    }


}
