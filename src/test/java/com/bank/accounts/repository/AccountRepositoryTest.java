package com.bank.accounts.repository;

import com.bank.accounts.entity.AccountsEntity;
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
    private AccountsRepository accountRepository;

    private AccountsEntity testAccount;

    @BeforeEach
    public void setUp() {
        testAccount = new AccountsEntity();
        testAccount.setAccountNumber(1234567890L);
        testAccount.setAccountType("SAVINGS");
        testAccount.setCustomerId(1L);
        testAccount.setBranchAddress("Main Branch");
    }

    @Test
    public void whenSaveAccount_thenReturnSavedAccount() {
        // Act
        AccountsEntity savedAccount = accountRepository.save(testAccount);
        
        // Assert
        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo(1234567890L);
    }

    @Test
    public void whenFindById_thenReturnAccount() {
        // Arrange
        AccountsEntity saved = entityManager.persistAndFlush(testAccount);
        
        // Act
        Optional<AccountsEntity> found = accountRepository.findById(saved.getAccountNumber());
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());
    }

    @Test
    public void whenFindByCustomerId_thenReturnAccount() {
        // Arrange
        entityManager.persistAndFlush(testAccount);
        
        // Act
        Optional<AccountsEntity> found = accountRepository.findByCustomerId(1L);
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(1L);
    }

    @Test
    public void whenFindAll_thenReturnAccountList() {
        // Arrange
        AccountsEntity account2 = new AccountsEntity();
        account2.setAccountNumber(987654321L);
        account2.setAccountType("CURRENT");
        account2.setCustomerId(2L);
        
        entityManager.persistAndFlush(testAccount);
        entityManager.persistAndFlush(account2);
        
        // Act
        List<AccountsEntity> accounts = accountRepository.findAll();
        
        // Assert
        assertThat(accounts).hasSize(2);
    }

    @Test
    public void whenDeleteAccount_thenAccountIsDeleted() {
        // Arrange
        AccountsEntity saved = entityManager.persistAndFlush(testAccount);
        Long accountId = saved.getAccountNumber();
        
        // Act
        accountRepository.deleteById(accountId);
        
        // Assert
        Optional<AccountsEntity> deleted = accountRepository.findById(accountId);
        assertThat(deleted).isEmpty();
    }

    @Test
    public void whenUpdateAccount_thenReturnUpdatedAccount() {
        // Arrange
        AccountsEntity saved = entityManager.persistAndFlush(testAccount);
        
        // Act
        saved.setAccountType("CURRENT");
        AccountsEntity updated = accountRepository.save(saved);
        
        // Assert
        assertThat(updated.getAccountType()).isEqualTo("CURRENT");
    }
}
