package com.bank.accounts.api;

import com.bank.accounts.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    public void setUp() {
        testAccount = new Account();
        testAccount.setAccountNumber("1234567890");
        testAccount.setAccountType("SAVINGS");
        testAccount.setCustomerId(1L);
        testAccount.setBranchAddress("Main Branch");
    }

    @Test
    public void whenSaveAccount_thenReturnSavedAccount() {
        // Act
        Account savedAccount = accountRepository.save(testAccount);
        
        // Assert
        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getAccountId()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("1234567890");
    }

    @Test
    public void whenFindById_thenReturnAccount() {
        // Arrange
        Account saved = entityManager.persistAndFlush(testAccount);
        
        // Act
        Optional<Account> found = accountRepository.findById(saved.getAccountId());
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());
    }

    @Test
    public void whenFindByCustomerId_thenReturnAccount() {
        // Arrange
        entityManager.persistAndFlush(testAccount);
        
        // Act
        Optional<Account> found = accountRepository.findByCustomerId(1L);
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(1L);
    }

    @Test
    public void whenFindAll_thenReturnAccountList() {
        // Arrange
        Account account2 = new Account();
        account2.setAccountNumber("0987654321");
        account2.setAccountType("CURRENT");
        account2.setCustomerId(2L);
        
        entityManager.persistAndFlush(testAccount);
        entityManager.persistAndFlush(account2);
        
        // Act
        List<Account> accounts = accountRepository.findAll();
        
        // Assert
        assertThat(accounts).hasSize(2);
    }

    @Test
    public void whenDeleteAccount_thenAccountIsDeleted() {
        // Arrange
        Account saved = entityManager.persistAndFlush(testAccount);
        Long accountId = saved.getAccountId();
        
        // Act
        accountRepository.deleteById(accountId);
        
        // Assert
        Optional<Account> deleted = accountRepository.findById(accountId);
        assertThat(deleted).isEmpty();
    }

    @Test
    public void whenUpdateAccount_thenReturnUpdatedAccount() {
        // Arrange
        Account saved = entityManager.persistAndFlush(testAccount);
        
        // Act
        saved.setAccountType("CURRENT");
        Account updated = accountRepository.save(saved);
        
        // Assert
        assertThat(updated.getAccountType()).isEqualTo("CURRENT");
    }
}
