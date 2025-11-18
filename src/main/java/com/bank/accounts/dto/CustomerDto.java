package com.bank.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(name = "Customer")
@Builder
@NoArgsConstructor  // Required for Jackson
@AllArgsConstructor // Required for Builder
public class CustomerDto {

    @NotEmpty(message = "Name can not be a null or empty")
    @Size(min = 3, max = 30, message = "The length of the name should be between 3 and 30")
    private String name;

    @NotEmpty(message = "E-mail can not be a null or empty")
    @Email(message = "The email should be valid")
    private String email;

    @NotEmpty(message = "Mobile number can not be a null or empty")
    @Pattern(regexp = "(^$|\\d{10})", message = "The mobile number must be 10 digit")
    private String mobileNumber;

    private AccountsDto accountsDto;
}