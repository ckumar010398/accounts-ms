package com.bank.accounts;

import com.bank.accounts.dto.AccountDto;
import com.bank.accounts.entity.Account;
import com.bank.accounts.exception.ResourceNotFoundException;
import com.bank.accounts.mapper.AccountMapper;
import com.bank.accounts.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account testAccount;
    private AccountDto testAccountDto;

    @BeforeEach
    public void setUp() {
        testAccount = new Account();
        testAccount.setAccountId(1L);
        testAccount.setAccountNumber("1234567890");
        testAccount.setAccountType("SAVINGS");
        testAccount.setCustomerId(100L);
        testAccount.setBranchAddress("Main Branch");

        testAccountDto = new AccountDto();
        testAccountDto.setAccountNumber("1234567890");
        testAccountDto.setAccountType("SAVINGS");
        testAccountDto.setCustomerId(100L);
        testAccountDto.setBranchAddress("Main Branch");
    }

    @Test
    public void givenAccountDto_whenCreateAccount_thenReturnCreatedAccount() {
        // Given
        given(accountMapper.toEntity(any(AccountDto.class))).willReturn(testAccount);
        given(accountRepository.save(any(Account.class))).willReturn(testAccount);
        given(accountMapper.toDto(any(Account.class))).willReturn(testAccountDto);

        // When
        AccountDto created = accountService.createAccount(testAccountDto);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getAccountNumber()).isEqualTo("1234567890");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void givenAccountId_whenGetAccountById_thenReturnAccount() {
        // Given
        given(accountRepository.findById(anyLong())).willReturn(Optional.of(testAccount));
        given(accountMapper.toDto(any(Account.class))).willReturn(testAccountDto);

        // When
        AccountDto found = accountService.getAccountById(1L);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getAccountNumber()).isEqualTo("1234567890");
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    public void givenInvalidAccountId_whenGetAccountById_thenThrowException() {
        // Given
        given(accountRepository.findById(anyLong())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
        
        verify(accountRepository, times(1)).findById(999L);
    }

    @Test
    public void givenAccountDto_whenUpdateAccount_thenReturnUpdatedAccount() {
        // Given
        testAccountDto.setAccountType("CURRENT");
        given(accountRepository.findById(anyLong())).willReturn(Optional.of(testAccount));
        given(accountRepository.save(any(Account.class))).willReturn(testAccount);
        given(accountMapper.toDto(any(Account.class))).willReturn(testAccountDto);

        // When
        AccountDto updated = accountService.updateAccount(1L, testAccountDto);

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getAccountType()).isEqualTo("CURRENT");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void givenAccountId_whenDeleteAccount_thenAccountIsDeleted() {
        // Given
        given(accountRepository.findById(anyLong())).willReturn(Optional.of(testAccount));
        willDoNothing().given(accountRepository).deleteById(anyLong());

        // When
        accountService.deleteAccount(1L);

        // Then
        verify(accountRepository, times(1)).deleteById(1L);
    }

    @Test
    public void givenCustomerId_whenGetAccountByCustomerId_thenReturnAccount() {
        // Given
        given(accountRepository.findByCustomerId(anyLong())).willReturn(Optional.of(testAccount));
        given(accountMapper.toDto(any(Account.class))).willReturn(testAccountDto);

        // When
        AccountDto found = accountService.getAccountByCustomerId(100L);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getCustomerId()).isEqualTo(100L);
        verify(accountRepository, times(1)).findByCustomerId(100L);
    }
}
