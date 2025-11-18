
package com.bank.accounts.repository;

import com.bank.accounts.audit.AuditAwareImpl;
import com.bank.accounts.entity.Customer;
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
@DisplayName("CustomerRepository Tests")
class CustomerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .name("John Doe")
                .email("john@example.com")
                .mobileNumber("9876543210")
                .build();
    }

    @Test
    @DisplayName("Save Customer Successfully")
    void testSaveCustomer() {
        // When
        Customer saved = customerRepository.save(customer);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getCustomerId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Find Customer By Mobile Number")
    void testFindByMobileNumber() {
        // Given
        entityManager.persist(customer);
        entityManager.flush();

        // When
        Optional<Customer> found = customerRepository.findByMobileNumber("9876543210");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @ParameterizedTest
    @DisplayName("Find Customers By Multiple Mobile Numbers")
    @CsvSource({
            "Alice, alice@example.com, 9111111111",
            "Bob, bob@example.com, 9222222222",
            "Charlie, charlie@example.com, 9333333333"
    })
    void testFindByMultipleMobileNumbers(String name, String email, String mobileNumber) {
        // Given
        Customer cust = Customer.builder()
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .build();
        entityManager.persist(cust);
        entityManager.flush();

        // When
        Optional<Customer> found = customerRepository.findByMobileNumber(mobileNumber);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(name);
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Find Customer By Non-Existent Mobile Number")
    void testFindByNonExistentMobileNumber() {
        // When
        Optional<Customer> found = customerRepository.findByMobileNumber("9999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Delete Customer By ID")
    void testDeleteById() {
        // Given
        Customer saved = entityManager.persist(customer);
        entityManager.flush();
        Long customerId = saved.getCustomerId();

        // When
        customerRepository.deleteById(customerId);
        entityManager.flush();

        // Then
        Optional<Customer> found = customerRepository.findById(customerId);
        assertThat(found).isEmpty();
    }
}