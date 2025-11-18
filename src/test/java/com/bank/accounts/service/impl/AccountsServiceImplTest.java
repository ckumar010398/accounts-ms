package com.bank.accounts.service.impl;

import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.entity.Accounts;
import com.bank.accounts.entity.Customer;
import com.bank.accounts.exception.CustomerAlreadyExistsException;
import com.bank.accounts.exception.ResourceNotFoundException;
import com.bank.accounts.mapper.AccountsMapper;
import com.bank.accounts.mapper.CustomerMapper;
import com.bank.accounts.repository.AccountsRepository;
import com.bank.accounts.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountsService Unit Tests")
class AccountsServiceImplTest {

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AccountsServiceImpl accountsService;

    private CustomerDto customerDto;
    private Customer customer;
    private Accounts accounts;
    private AccountsDto accountsDto;

    @BeforeEach
    void setUp() {
        // Initialize test data
        customerDto = CustomerDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("9876543210")
                .build();

        customer = Customer.builder()
                .customerId(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("9876543210")
                .build();

        accounts = Accounts.builder()
                .accountNumber(1000000001L)
                .customerId(1L)
                .accountType("Savings")
                .branchAddress("123 Main St")
                .build();

        accountsDto = AccountsDto.builder()
                .accountNumber(1000000001L)
                .accountType("Savings")
                .branchAddress("123 Main St")
                .build();
    }

    @Test
    @DisplayName("Create Account - Success")
    void testCreateAccount_Success() {
        // Given
        when(customerRepository.findByMobileNumber(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(accountsRepository.save(any(Accounts.class))).thenReturn(accounts);

        // Mock static mapper method
        try (MockedStatic<CustomerMapper> customerMapperMock = mockStatic(CustomerMapper.class);
             MockedStatic<AccountsMapper> accountsMapperMock = mockStatic(AccountsMapper.class)) {

            customerMapperMock.when(() -> CustomerMapper.mapToCustomerEntity(any(CustomerDto.class), any(Customer.class)))
                    .thenReturn(customer);

            // When
            accountsService.createAccount(customerDto);

            // Then
            verify(customerRepository, times(1)).findByMobileNumber(customerDto.getMobileNumber());
            verify(customerRepository, times(1)).save(any(Customer.class));
            verify(accountsRepository, times(1)).save(any(Accounts.class));

            ArgumentCaptor<Accounts> accountsCaptor = ArgumentCaptor.forClass(Accounts.class);
            verify(accountsRepository).save(accountsCaptor.capture());
            Accounts savedAccount = accountsCaptor.getValue();

            assertThat(savedAccount.getAccountType()).isEqualTo("Savings");
            assertThat(savedAccount.getAccountNumber()).isPositive();
        }
    }

    @Test
    @DisplayName("Create Account - Customer Already Exists")
    void testCreateAccount_CustomerAlreadyExists() {
        // Given
        when(customerRepository.findByMobileNumber(anyString())).thenReturn(Optional.of(customer));

        // When & Then
        assertThatThrownBy(() -> accountsService.createAccount(customerDto))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("already registered");

        verify(customerRepository, times(1)).findByMobileNumber(customerDto.getMobileNumber());
        verify(customerRepository, never()).save(any(Customer.class));
        verify(accountsRepository, never()).save(any(Accounts.class));
    }

    @ParameterizedTest
    @DisplayName("Fetch Account - Valid Mobile Numbers")
    @ValueSource(strings = {"9876543210", "8765432109", "7654321098"})
    void testFetchAccount_ValidMobileNumbers(String mobileNumber) {
        // Given
        customer.setMobileNumber(mobileNumber);
        customerDto.setMobileNumber(mobileNumber);

        when(customerRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.of(customer));
        when(accountsRepository.findByCustomerId(anyLong())).thenReturn(Optional.of(accounts));

        // Mock static mapper methods
        try (MockedStatic<CustomerMapper> customerMapperMock = mockStatic(CustomerMapper.class);
             MockedStatic<AccountsMapper> accountsMapperMock = mockStatic(AccountsMapper.class)) {

            customerMapperMock.when(() -> CustomerMapper.mapToCustomerDto(any(Customer.class), any(CustomerDto.class)))
                    .thenReturn(customerDto);
            accountsMapperMock.when(() -> AccountsMapper.mapToAccountsDto(any(Accounts.class), any(AccountsDto.class)))
                    .thenReturn(accountsDto);

            // When
            CustomerDto result = accountsService.fetchAccount(mobileNumber);

            // Then
            assertThat(result).isNotNull();
            verify(customerRepository, times(1)).findByMobileNumber(mobileNumber);
            verify(accountsRepository, times(1)).findByCustomerId(customer.getCustomerId());
        }
    }

    @Test
    @DisplayName("Fetch Account - Customer Not Found")
    void testFetchAccount_CustomerNotFound() {
        // Given
        String mobileNumber = "9999999999";
        when(customerRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountsService.fetchAccount(mobileNumber))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("not found");

        verify(customerRepository, times(1)).findByMobileNumber(mobileNumber);
        verify(accountsRepository, never()).findByCustomerId(anyLong());
    }

    @Test
    @DisplayName("Update Account - Success")
    void testUpdateAccount_Success() {
        // Given
        CustomerDto updateDto = CustomerDto.builder()
                .name("John Updated")
                .email("john.updated@example.com")
                .mobileNumber("9876543210")
                .accountsDto(accountsDto)
                .build();

        when(accountsRepository.findById(anyLong())).thenReturn(Optional.of(accounts));
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(customer));
        when(accountsRepository.save(any(Accounts.class))).thenReturn(accounts);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Mock static mapper methods
        try (MockedStatic<AccountsMapper> accountsMapperMock = mockStatic(AccountsMapper.class);
             MockedStatic<CustomerMapper> customerMapperMock = mockStatic(CustomerMapper.class)) {

            accountsMapperMock.when(() -> AccountsMapper.mapToAccountsEntity(any(AccountsDto.class), any(Accounts.class)))
                    .thenReturn(accounts);
            customerMapperMock.when(() -> CustomerMapper.mapToCustomerEntity(any(CustomerDto.class), any(Customer.class)))
                    .thenReturn(customer);

            // When
            boolean result = accountsService.updateAccount(updateDto);

            // Then
            assertThat(result).isTrue();
            verify(accountsRepository, times(1)).findById(accountsDto.getAccountNumber());
            verify(customerRepository, times(1)).findById(accounts.getCustomerId());
            verify(accountsRepository, times(1)).save(any(Accounts.class));
            verify(customerRepository, times(1)).save(any(Customer.class));
        }
    }

    @ParameterizedTest
    @DisplayName("Update Account - Different Account Types")
    @CsvSource({
            "Savings, 123 Main St",
            "Current, 456 Oak Ave",
            "Fixed Deposit, 789 Pine Rd"
    })
    void testUpdateAccount_DifferentAccountTypes(String accountType, String branchAddress) {
        // Given
        accountsDto.setAccountType(accountType);
        accountsDto.setBranchAddress(branchAddress);

        CustomerDto updateDto = CustomerDto.builder()
                .mobileNumber("9876543210")
                .accountsDto(accountsDto)
                .build();

        Accounts updatedAccounts = Accounts.builder()
                .accountNumber(accountsDto.getAccountNumber())
                .customerId(1L)
                .accountType(accountType)
                .branchAddress(branchAddress)
                .build();

        when(accountsRepository.findById(anyLong())).thenReturn(Optional.of(accounts));
        when(customerRepository.findById(anyLong())).thenReturn(Optional.of(customer));
        when(accountsRepository.save(any(Accounts.class))).thenReturn(updatedAccounts);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Mock static mapper methods
        try (MockedStatic<AccountsMapper> accountsMapperMock = mockStatic(AccountsMapper.class);
             MockedStatic<CustomerMapper> customerMapperMock = mockStatic(CustomerMapper.class)) {

            accountsMapperMock.when(() -> AccountsMapper.mapToAccountsEntity(any(AccountsDto.class), any(Accounts.class)))
                    .thenReturn(updatedAccounts);
            customerMapperMock.when(() -> CustomerMapper.mapToCustomerEntity(any(CustomerDto.class), any(Customer.class)))
                    .thenReturn(customer);

            // When
            boolean result = accountsService.updateAccount(updateDto);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("Update Account - Account Not Found")
    void testUpdateAccount_AccountNotFound() {
        // Given
        when(accountsRepository.findById(anyLong())).thenReturn(Optional.empty());

        CustomerDto updateDto = CustomerDto.builder()
                .accountsDto(accountsDto)
                .build();

        // When & Then
        assertThatThrownBy(() -> accountsService.updateAccount(updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account");

        verify(accountsRepository, times(1)).findById(anyLong());
        verify(customerRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Delete Account - Success")
    void testDeleteAccount_Success() {
        // Given
        String mobileNumber = "9876543210";
        when(customerRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.of(customer));

        // When
        boolean result = accountsService.deleteAccount(mobileNumber);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository, times(1)).findByMobileNumber(mobileNumber);
        verify(accountsRepository, times(1)).deleteByCustomerId(customer.getCustomerId());
        verify(customerRepository, times(1)).deleteById(customer.getCustomerId());
    }

    @ParameterizedTest
    @DisplayName("Delete Account - Multiple Mobile Numbers")
    @ValueSource(strings = {"9876543210", "8765432109", "7654321098"})
    void testDeleteAccount_MultipleMobileNumbers(String mobileNumber) {
        // Given
        customer.setMobileNumber(mobileNumber);
        when(customerRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.of(customer));

        // When
        boolean result = accountsService.deleteAccount(mobileNumber);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository, times(1)).findByMobileNumber(mobileNumber);
    }

    @Test
    @DisplayName("Delete Account - Customer Not Found")
    void testDeleteAccount_CustomerNotFound() {
        // Given
        String mobileNumber = "9999999999";
        when(customerRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountsService.deleteAccount(mobileNumber))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, times(1)).findByMobileNumber(mobileNumber);
        verify(accountsRepository, never()).deleteByCustomerId(anyLong());
    }
}
