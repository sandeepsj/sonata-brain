package com.example.sonata_brain.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates a first administrator from configuration, so a fresh database is usable without SQL (REQ-015). */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final UserService users;
    private final String email;
    private final String password;

    public BootstrapAdmin(UserService users,
                          @Value("${sonata.bootstrap-admin.email:}") String email,
                          @Value("${sonata.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        users.ensureAdmin(email.trim(), password);
        log.info("Bootstrap administrator {} is present", email);
    }
}
