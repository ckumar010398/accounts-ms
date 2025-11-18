package com.bank.accounts.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contact-info")
public record ContactInfoDto() {
}
