package com.bank.accounts.repository;

import com.bank.accounts.audit.AuditAwareImpl;
import com.bank.accounts.entity.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(AuditAwareImpl.class)
@DisplayName("AccountsRepository Tests")
class AccountsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountsRepository accountsRepository;

    private Accounts accounts;

    @BeforeEach
    void setUp() {
        accounts = Accounts.builder()
                .accountNumber(1000000001L)
                .customerId(1L)
                .accountType("Savings")
                .branchAddress("123 Main St")
                .build();
    }

    @Test
    @DisplayName("Save Account Successfully")
    void testSaveAccount() {
        // When
        Accounts saved = accountsRepository.save(accounts);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getAccountNumber()).isEqualTo(1000000001L);
        assertThat(saved.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Find Account By Customer ID")
    void testFindByCustomerId() {
        // Given
        entityManager.persist(accounts);
        entityManager.flush();

        // When
        Optional<Accounts> found = accountsRepository.findByCustomerId(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(1000000001L);
    }

    @ParameterizedTest
    @DisplayName("Find Accounts By Multiple Customer IDs")
    @CsvSource({
            "1, 1000000001, Savings",
            "2, 1000000002, Current",
            "3, 1000000003, Fixed Deposit"
    })
    void testFindByMultipleCustomerIds(Long customerId, Long accountNumber, String accountType) {
        // Given
        Accounts account = Accounts.builder()
                .accountNumber(accountNumber)
                .customerId(customerId)
                .accountType(accountType)
                .branchAddress("Test Branch")
                .build();
        entityManager.persist(account);
        entityManager.flush();

        // When
        Optional<Accounts> found = accountsRepository.findByCustomerId(customerId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(found.get().getAccountType()).isEqualTo(accountType);
    }

    @Test
    @DisplayName("Delete Account By Customer ID")
    void testDeleteByCustomerId() {
        // Given
        entityManager.persist(accounts);
        entityManager.flush();

        // When
        accountsRepository.deleteByCustomerId(1L);
        entityManager.flush();

        // Then
        Optional<Accounts> found = accountsRepository.findByCustomerId(1L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Find Account By ID")
    void testFindById() {
        // Given
        entityManager.persist(accounts);
        entityManager.flush();

        // When
        Optional<Accounts> found = accountsRepository.findById(1000000001L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(1L);
    }
}