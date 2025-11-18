package com.bank.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "Customer", description = "Schema holds the customer and account information")
public class CustomerDto {

    @NotEmpty(message = "Name can not be a null or empty")
    @Size(min = 3, max = 30, message = "The length of the name should be between 3 and 30")
    @Schema(description = "Name of the customer", example = "Chandan")
    private String name;

    @NotEmpty(message = "E-mail can not be a null or empty")
    @Email(message = "The email should be valid")
    @Schema(description = "Email of the customer", example = "chandan03@zohomail.in")
    private String email;

    @NotEmpty(message = "Mobile number can not be a null or empty")
    @Pattern(regexp = "(^$|\\d{10})", message = "The mobile number must be 10 digit")
    @Schema(description = "Mobile number of the customer", example = "8978767656")
    private String mobileNumber;
    private AccountsDto accountsDto;

}
