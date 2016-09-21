package com.sk.config;

import com.sk.service.ScriptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"com.sk",})
public class AppConfig {

    @Bean(name = "scriptService")
    public ScriptService scriptService(){
        return new ScriptService();
    }

}
