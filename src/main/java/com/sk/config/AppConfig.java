package com.sk.config;

import com.sk.executor.UserScriptExecutor;
import com.sk.executor.UserScriptExecutorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.sk")
public class AppConfig {

    @Bean(name = "scriptExecutor", destroyMethod = "shutdown")
    public UserScriptExecutor scriptExecutor() {
        return new UserScriptExecutorImpl(5);
    }

}
