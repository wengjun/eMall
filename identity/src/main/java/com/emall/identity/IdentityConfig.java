package com.emall.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class IdentityConfig {
    @Bean
    PasswordEncoder identityPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
