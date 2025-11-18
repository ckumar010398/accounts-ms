
package com.bank.accounts.integration;

import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.entity.Accounts;
import com.bank.accounts.entity.Customer;
import com.bank.accounts.repository.AccountsRepository;
import com.bank.accounts.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Accounts Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountsRepository accountsRepository;

    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        customerDto = CustomerDto.builder()
                .name("Integration Test User")
                .email("integration@test.com")
                .mobileNumber("9999888877")
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Integration: Create Account End-to-End")
    void testCreateAccount_EndToEnd() throws Exception {
        // When
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is("201")));

        // Then - Verify in database
        Customer savedCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber())
                .orElseThrow();
        assertThat(savedCustomer.getName()).isEqualTo(customerDto.getName());
        assertThat(savedCustomer.getEmail()).isEqualTo(customerDto.getEmail());

        Accounts savedAccount = accountsRepository.findByCustomerId(savedCustomer.getCustomerId())
                .orElseThrow();
        assertThat(savedAccount.getAccountType()).isEqualTo("Savings");
        assertThat(savedAccount.getAccountNumber()).isPositive();
    }

    @ParameterizedTest
    @Order(2)
    @DisplayName("Integration: Create Multiple Accounts")
    @CsvSource({
            "Alice Johnson, alice@example.com, 9111111111",
            "Bob Smith, bob@example.com, 9222222222",
            "Charlie Brown, charlie@example.com, 9333333333"
    })
    void testCreateMultipleAccounts(String name, String email, String mobileNumber) throws Exception {
        // Given
        CustomerDto dto = CustomerDto.builder()
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .build();

        // When
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Then
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow();
        assertThat(customer.getName()).isEqualTo(name);
        assertThat(customer.getEmail()).isEqualTo(email);
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Fetch Account End-to-End")
    void testFetchAccount_EndToEnd() throws Exception {
        // Given - Create account first
        Customer customer = Customer.builder()
                .name("Fetch Test User")
                .email("fetch@test.com")
                .mobileNumber("9888777666")
                .build();
        customer = customerRepository.save(customer);

        Accounts accounts = Accounts.builder()
                .customerId(customer.getCustomerId())
                .accountNumber(1000000999L)
                .accountType("Savings")
                .branchAddress("123 Test St")
                .build();
        accountsRepository.save(accounts);

        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "9888777666"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Fetch Test User")))
                .andExpect(jsonPath("$.email", is("fetch@test.com")))
                .andExpect(jsonPath("$.accountsDto.accountNumber", is(1000000999)))
                .andExpect(jsonPath("$.accountsDto.accountType", is("Savings")));
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Update Account End-to-End")
    void testUpdateAccount_EndToEnd() throws Exception {
        // Given - Create account
        Customer customer = Customer.builder()
                .name("Update Test User")
                .email("update@test.com")
                .mobileNumber("9777666555")
                .build();
        customer = customerRepository.save(customer);

        Accounts accounts = Accounts.builder()
                .customerId(customer.getCustomerId())
                .accountNumber(1000000888L)
                .accountType("Savings")
                .branchAddress("Old Address")
                .build();
        accountsRepository.save(accounts);

        // Prepare update DTO
        AccountsDto accountsDto = AccountsDto.builder()
                .accountNumber(1000000888L)
                .accountType("Current")
                .branchAddress("New Address 456")
                .build();

        CustomerDto updateDto = CustomerDto.builder()
                .name("Updated Name")
                .email("updated@test.com")
                .mobileNumber("9777666555")
                .accountsDto(accountsDto)
                .build();

        // When
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is("200")));

        // Then - Verify changes in database
        Accounts updatedAccount = accountsRepository.findById(1000000888L).orElseThrow();
        assertThat(updatedAccount.getAccountType()).isEqualTo("Current");
        assertThat(updatedAccount.getBranchAddress()).isEqualTo("New Address 456");

        Customer updatedCustomer = customerRepository.findById(customer.getCustomerId()).orElseThrow();
        assertThat(updatedCustomer.getName()).isEqualTo("Updated Name");
        assertThat(updatedCustomer.getEmail()).isEqualTo("updated@test.com");
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Delete Account End-to-End")
    void testDeleteAccount_EndToEnd() throws Exception {
        // Given
        Customer customer = Customer.builder()
                .name("Delete Test User")
                .email("delete@test.com")
                .mobileNumber("9666555444")
                .build();
        customer = customerRepository.save(customer);

        Accounts accounts = Accounts.builder()
                .customerId(customer.getCustomerId())
                .accountNumber(1000000777L)
                .accountType("Savings")
                .branchAddress("Delete Address")
                .build();
        accountsRepository.save(accounts);

        // When
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9666555444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is("200")));

        // Then - Verify deletion
        assertThat(customerRepository.findByMobileNumber("9666555444")).isEmpty();
        assertThat(accountsRepository.findByCustomerId(customer.getCustomerId())).isEmpty();
    }

    @RepeatedTest(value = 3, name = "Create-Fetch-Delete Cycle - Iteration {currentRepetition}")
    @Order(6)
    @DisplayName("Integration: Complete Workflow Repeated Test")
    void testCompleteWorkflow_RepeatedTest(RepetitionInfo repetitionInfo) throws Exception {
        String mobileNumber = "9" + String.format("%09d", repetitionInfo.getCurrentRepetition());
        
        CustomerDto dto = CustomerDto.builder()
                .name("Repeat User " + repetitionInfo.getCurrentRepetition())
                .email("repeat" + repetitionInfo.getCurrentRepetition() + "@test.com")
                .mobileNumber(mobileNumber)
                .build();

        // Step 1: Create
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Step 2: Fetch
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobileNumber", is(mobileNumber)));

        // Step 3: Delete
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk());

        // Verify deletion
        assertThat(customerRepository.findByMobileNumber(mobileNumber)).isEmpty();
    }
}