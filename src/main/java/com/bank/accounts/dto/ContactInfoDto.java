package com.bank.accounts.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "contact-info")
public class ContactInfoDto {

    private String message;
    private ContactDetails contactDetails;
    private List<String> onCallSupport;

    @Data
    public static class ContactDetails {
        private String name;
        private String email;
    }
}