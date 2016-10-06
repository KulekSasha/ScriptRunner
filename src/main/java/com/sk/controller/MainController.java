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
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    
    //TODO duplication of same constant in scriptexecutor 
    private static final String VALIDATION_OK = "valid";

    @Resource
    private UserScriptExecutor scriptExecutor;

    @RequestMapping(value = "/scripts", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> addNewScriptPlainText(@RequestBody String input, UriComponentsBuilder ucBuilder) {
        LOG.debug("Input: {{}}", input);
        String validationResult = scriptExecutor.validate(input);

        // TODO consider a combination of throwing validation exception from business layer, and exception mapper to map it to response.
        if (!validationResult.equals(VALIDATION_OK)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("CompilationError", validationResult)
                    .build();
        }

        UserScript userScript = new UserScript();
        
        // TODO compiled scripts execute much faster than text scripts, because text scripts have to be compiled again 
        userScript.setScript(input);
        long newId = scriptExecutor.add(userScript);

        return ResponseEntity.created(ucBuilder.path("/scripts/{id}").buildAndExpand(newId).toUri()).build();
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getScriptById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getByIdWoDetails(id);
        
        // TODO this check is a code duplication, consider using exception and exception mapper instead
        if (uScript == null) {
            return notFound().build();
        }
        
        return ResponseEntity.ok(uScript);
    }

    @RequestMapping(value = "/scripts/{id}/request", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> getScriptStringById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getByIdWoDetails(id);
        if (uScript == null) {
            return ResponseEntity.notFound().build();
        }
        
        // TODO try learning cache control header, because text does not change since script is sent to api
        return ok(uScript.getScript());
    }

    @RequestMapping(value = "/scripts/{id}/result", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> getScriptResultById(@PathVariable("id") long id) {
        UserScript uScript = scriptExecutor.getById(id);
        if (uScript == null) {
            return ResponseEntity.notFound().build();
        }
        return ok(uScript.getResult());
    }

    @RequestMapping(value = "/scripts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserScript>> getAllScripts() {
        List<UserScript> uScripts = scriptExecutor.getAll();
        return ok(uScripts);
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteScript(@PathVariable("id") long id) {
        if (scriptExecutor.deleteById(id)) {
            return ResponseEntity.noContent().build();
        } else {
        	// TODO also a case for a combination of not found exception plus exception mapper 
            return notFound().build();
        }
    }

}
