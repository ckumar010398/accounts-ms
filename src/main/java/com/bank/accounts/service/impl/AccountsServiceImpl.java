package com.bank.accounts.service.impl;

import com.bank.accounts.constants.AccountsConstants;
import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.entity.AccountsEntity;
import com.bank.accounts.entity.CustomerEntity;
import com.bank.accounts.exception.CustomerAlreadyExistsException;
import com.bank.accounts.exception.ResourceNotFoundException;
import com.bank.accounts.mapper.AccountsMapper;
import com.bank.accounts.mapper.CustomerMapper;
import com.bank.accounts.repository.AccountsRepository;
import com.bank.accounts.repository.CustomerRepository;
import com.bank.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;

    @Override
    public void createAccount(CustomerDto customerDto) {

       CustomerEntity customer = CustomerMapper.mapToCustomerEntity(customerDto, new CustomerEntity());

        Optional<CustomerEntity> optionalCustomerEntity = customerRepository.findByMobileNumber(customerDto.getMobileNumber());
        if(optionalCustomerEntity.isPresent()){
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "+customerDto.getMobileNumber());
        }

        CustomerEntity savedCustomer = customerRepository.save(customer);
        accountsRepository.save(createNewAccount(savedCustomer));

    }

    private AccountsEntity createNewAccount(CustomerEntity savedCustomer) {

        AccountsEntity  accounts = new AccountsEntity();

        Random random = new Random();

        int accountNumber = random.nextInt(90000000);
        Long randomAccountNumber = 1000000000L + accountNumber;

        accounts.setAccountNumber(randomAccountNumber);
        accounts.setCustomerId(savedCustomer.getCustomerId());
        accounts.setAccountType(AccountsConstants.SAVINGS);
        accounts.setBranchAddress(AccountsConstants.ADDRESS);

        return accounts;
    }

    @Override
    public CustomerDto fetchAccount(String mobileNumber) {

        CustomerEntity customer = customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        AccountsEntity accounts = accountsRepository.findByCustomerId(customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Accounts", "customerId", customer.getCustomerId().toString()));

        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer,new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts,new AccountsDto()));

        return customerDto;
    }

    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();

        if(null!=accountsDto){
            AccountsEntity accounts = accountsRepository.findById(accountsDto.getAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Accounts", "customerId", accountsDto.getAccountNumber().toString()));

            AccountsMapper.mapToAccountsEntity(accountsDto,accounts);
            accounts = accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();

            CustomerEntity customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId.toString()));

            CustomerMapper.mapToCustomerEntity(customerDto,customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return isUpdated;
    }

    @Override
    public boolean deleteAccount(String mobileNumber) {

        CustomerEntity customer = customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow( () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());

        return true;
    }
}
