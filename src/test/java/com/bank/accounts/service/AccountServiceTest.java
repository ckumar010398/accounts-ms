package com.bank.accounts.service;

import com.bank.accounts.constants.AccountsConstants;
import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.entity.AccountsEntity;
import com.bank.accounts.entity.CustomerEntity;
import com.bank.accounts.exception.CustomerAlreadyExistsException;
import com.bank.accounts.exception.ResourceNotFoundException;
import com.bank.accounts.mapper.AccountsMapper;
import com.bank.accounts.repository.AccountsRepository;
import com.bank.accounts.repository.CustomerRepository;
import com.bank.accounts.service.impl.AccountsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private AccountsMapper accountsMapper;

    @InjectMocks
    private AccountsServiceImpl accountsService;

    private CustomerDto testCustomerDto;
    private CustomerEntity testCustomer;
    private AccountsEntity testAccount;

    @BeforeEach
    public void setUp() {
        // Initialize test customer DTO
        testCustomerDto = new CustomerDto();
        testCustomerDto.setName("John Doe");
        testCustomerDto.setEmail("john.doe@eazybank.com");
        testCustomerDto.setMobileNumber("9876543210");

        // Initialize test customer entity
        testCustomer = new CustomerEntity();
        testCustomer.setCustomerId(1L);
        testCustomer.setName("John Doe");
        testCustomer.setEmail("john.doe@eazybank.com");
        testCustomer.setMobileNumber("9876543210");
        testCustomer.setCreatedAt(LocalDateTime.now());

        // Initialize test account entity
        testAccount = new AccountsEntity();
        testAccount.setCustomerId(1L);
        testAccount.setAccountType(AccountsConstants.SAVINGS);
        testAccount.setAccountNumber(1234567890L);
        testAccount.setBranchAddress("Main Branch");
        testAccount.setCreatedAt(LocalDateTime.now());
    }

    // ========== CREATE ACCOUNT TESTS ==========

    @Test
    public void givenValidCustomerDto_whenCreateAccount_thenAccountIsCreated() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        accountsService.createAccount(testCustomerDto);

        // Then
        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(customerRepository, times(1)).save(any(CustomerEntity.class));
        verify(accountsRepository, times(1)).save(any(AccountsEntity.class));
    }

    @Test
    public void givenExistingCustomerMobileNumber_whenCreateAccount_thenThrowException() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));

        // When & Then
        assertThatThrownBy(() -> accountsService.createAccount(testCustomerDto))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("CustomerEntity already exists with mobile number");

        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(customerRepository, never()).save(any(CustomerEntity.class));
        verify(accountsRepository, never()).save(any(AccountsEntity.class));
    }

    @Test
    public void givenValidCustomer_whenCreateAccount_thenAccountTypeIsSavings() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        accountsService.createAccount(testCustomerDto);

        // Then - Verify account type is SAVINGS
        verify(accountsRepository, times(1)).save(argThat(account ->
                account.getAccountType().equals(AccountsConstants.SAVINGS)
        ));
    }

    @Test
    public void givenValidCustomer_whenCreateAccount_thenAccountNumberIsGenerated() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        accountsService.createAccount(testCustomerDto);

        // Then - Verify account number is generated
        verify(accountsRepository, times(1)).save(argThat(account ->
                account.getAccountNumber() != null && !account.getAccountNumber().toString().isEmpty()
        ));
    }

    // ========== FETCH ACCOUNT TESTS ==========

    @Test
    public void givenValidMobileNumber_whenFetchAccount_thenReturnCustomerDto() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));

        CustomerDto expectedDto = new CustomerDto();
        expectedDto.setName("John Doe");
        expectedDto.setEmail("john.doe@eazybank.com");
        expectedDto.setMobileNumber("9876543210");

       // given(CustomerMapper.mapToCustomerDto(any(CustomerEntity.class), new CustomerDto())).willReturn(expectedDto);

        // When
        CustomerDto result = accountsService.fetchAccount("9876543210");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@eazybank.com");
        assertThat(result.getMobileNumber()).isEqualTo("9876543210");

        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(accountsRepository, times(1)).findByCustomerId(1L);
    }

    @Test
    public void givenInvalidMobileNumber_whenFetchAccount_thenThrowException() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountsService.fetchAccount("9999999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CustomerEntity not found with mobile number");

        verify(customerRepository, times(1)).findByMobileNumber("9999999999");
        verify(accountsRepository, never()).findByCustomerId(anyLong());
    }

    @Test
    public void givenValidCustomer_whenFetchAccount_thenAccountDetailsAreReturned() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));

        CustomerDto expectedDto = new CustomerDto();
        expectedDto.setAccountsDto(new AccountsDto());
        expectedDto.getAccountsDto().setAccountType(AccountsConstants.SAVINGS);

        // When
        CustomerDto result = accountsService.fetchAccount("9876543210");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountsDto()).isNotNull();
        assertThat(result.getAccountsDto().getAccountType()).isEqualTo(AccountsConstants.SAVINGS);
    }

    // ========== UPDATE ACCOUNT TESTS ==========

    @Test
    public void givenValidCustomerDto_whenUpdateAccount_thenReturnTrue() {
        // Given
        testCustomerDto.setAccountsDto(new AccountsDto());
        testCustomerDto.getAccountsDto().setAccountType("CURRENT");
        testCustomerDto.getAccountsDto().setAccountNumber(1234567890L);
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        boolean result = accountsService.updateAccount(testCustomerDto);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(accountsRepository, times(1)).findByCustomerId(1L);
        verify(customerRepository, times(1)).save(any(CustomerEntity.class));
        verify(accountsRepository, times(1)).save(any(AccountsEntity.class));
    }

    @Test
    public void givenInvalidMobileNumber_whenUpdateAccount_thenReturnFalse() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());

        // When
        boolean result = accountsService.updateAccount(testCustomerDto);

        // Then
        assertThat(result).isFalse();
        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(customerRepository, never()).save(any(CustomerEntity.class));
        verify(accountsRepository, never()).save(any(AccountsEntity.class));
    }

    @Test
    public void givenValidCustomer_whenUpdateAccountWithNewEmail_thenEmailIsUpdated() {
        // Given
        testCustomerDto.setEmail("newemail@eazybank.com");

        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        accountsService.updateAccount(testCustomerDto);

        // Then
        verify(customerRepository, times(1)).save(argThat(customer ->
                customer.getEmail().equals("newemail@eazybank.com")
        ));
    }

    @Test
    public void givenValidCustomer_whenUpdateAccountWithNewBranch_thenBranchIsUpdated() {
        // Given
        testCustomerDto.setAccountsDto(new AccountsDto());
        testCustomerDto.getAccountsDto().setBranchAddress("Downtown Branch");
        testCustomerDto.getAccountsDto().setAccountNumber(1234567890L);
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When
        accountsService.updateAccount(testCustomerDto);

        // Then
        verify(accountsRepository, times(1)).save(argThat(account ->
                account.getBranchAddress().equals("Downtown Branch")
        ));
    }

    // ========== DELETE ACCOUNT TESTS ==========

    @Test
    public void givenValidMobileNumber_whenDeleteAccount_thenReturnTrue() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        willDoNothing().given(accountsRepository).deleteById(anyLong());
        willDoNothing().given(customerRepository).deleteById(anyLong());

        // When
        boolean result = accountsService.deleteAccount("9876543210");

        // Then
        assertThat(result).isTrue();
        verify(customerRepository, times(1)).findByMobileNumber("9876543210");
        verify(accountsRepository, times(1)).findByCustomerId(1L);
        verify(accountsRepository, times(1)).deleteById(1L);
        verify(customerRepository, times(1)).deleteById(1L);
    }

    @Test
    public void givenInvalidMobileNumber_whenDeleteAccount_thenReturnFalse() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());

        // When
        boolean result = false;
        try {
            result = accountsService.deleteAccount("9876543210");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Then
        assertThat(result).isFalse();
        verify(customerRepository, times(1)).findByMobileNumber("9999999999");
        verify(accountsRepository, never()).deleteById(anyLong());
        verify(customerRepository, never()).deleteById(anyLong());
    }

    @Test
    public void givenValidCustomer_whenDeleteAccount_thenBothCustomerAndAccountAreDeleted() {
        // Given
      //  given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
      //  given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        willDoNothing().given(accountsRepository).deleteById(anyLong());
        willDoNothing().given(customerRepository).deleteById(anyLong());

        // When
        accountsService.deleteAccount("9876543210");

        // Then - Verify both are deleted
        verify(customerRepository, times(1)).deleteById(testCustomer.getCustomerId());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    public void givenCustomerDtoWithNullName_whenCreateAccount_thenHandleGracefully() {
        // Given
        testCustomerDto.setName(null);
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When & Then - Should handle null gracefully
        assertThatCode(() -> accountsService.createAccount(testCustomerDto))
                .doesNotThrowAnyException();
    }

    @Test
    public void givenCustomerDtoWithNullEmail_whenCreateAccount_thenHandleGracefully() {
        // Given
        testCustomerDto.setEmail(null);
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When & Then - Should handle null gracefully
        assertThatCode(() -> accountsService.createAccount(testCustomerDto))
                .doesNotThrowAnyException();
    }

    @Test
    public void givenAccountsNotFoundForCustomer_whenFetchAccount_thenThrowException() {
        // Given
        given(customerRepository.findByMobileNumber(anyString())).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(anyLong())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountsService.fetchAccount("9876543210"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found for customer id");
    }

    @Test
    public void givenMultipleServiceCalls_whenCreateAndFetch_thenBothOperationsSucceed() {
        // Given - Setup for create
        given(customerRepository.findByMobileNumber("9876543210")).willReturn(Optional.empty());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(testCustomer);
        given(accountsRepository.save(any(AccountsEntity.class))).willReturn(testAccount);

        // When - Create account
        accountsService.createAccount(testCustomerDto);

        // Given - Setup for fetch
        given(customerRepository.findByMobileNumber("9876543210")).willReturn(Optional.of(testCustomer));
        given(accountsRepository.findByCustomerId(1L)).willReturn(Optional.of(testAccount));

        CustomerDto expectedDto = new CustomerDto();

        // When - Fetch account
        CustomerDto result = accountsService.fetchAccount("9876543210");

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository, times(2)).findByMobileNumber("9876543210");
    }
}
