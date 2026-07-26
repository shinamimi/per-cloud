package com.cloud.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String host;
    private int port;
    private String username;
    private String password;
}