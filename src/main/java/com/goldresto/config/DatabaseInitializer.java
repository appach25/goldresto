package com.goldresto.config;

import com.goldresto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) {
        // Initialize roles first
        userService.initializeRoles();
        
        // Then create default users
        userService.initializeDefaultUsers();
    }
}
