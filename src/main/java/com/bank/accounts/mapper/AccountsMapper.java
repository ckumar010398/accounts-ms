package com.bank.accounts.mapper;

import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.entity.AccountsEntity;

public class AccountsMapper {

    public static AccountsDto mapToAccountsDto(AccountsEntity accountsEntity, AccountsDto accountsDto){

        accountsDto.setAccountNumber(accountsEntity.getAccountNumber());
        accountsDto.setAccountType(accountsEntity.getAccountType());
        accountsDto.setBranchAddress(accountsEntity.getBranchAddress());

        return accountsDto;
    }

    public static AccountsEntity mapToAccountsEntity(AccountsDto accountsDto, AccountsEntity accountsEntity){

        accountsEntity.setAccountNumber(accountsDto.getAccountNumber());
        accountsEntity.setAccountType(accountsDto.getAccountType());
        accountsEntity.setBranchAddress(accountsDto.getBranchAddress());

        return accountsEntity;
    }
}
