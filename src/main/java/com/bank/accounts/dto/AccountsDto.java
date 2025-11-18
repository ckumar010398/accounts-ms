package com.bank.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(
        name = "Accounts",
        description = "Schema holds account information"
)
public class AccountsDto {

    @NotEmpty(message = "The account number can't be a null or empty")
    @Pattern(regexp = "(^$|\\d{10})", message = "Account number must be 10 digits")
    @Schema(description = "Account number of bank account",example = "4533423431")
    private Long accountNumber;

    @NotEmpty(message = "The account type can't be a null or empty")
    @Schema(description = "Account type of bank account",example = "Savings")
    private String accountType;

    @NotEmpty(message = "The branch address can't be a null or empty")
    @Schema(description = "Branch address of bank account",example = "4533423431")
    private String branchAddress;

}
