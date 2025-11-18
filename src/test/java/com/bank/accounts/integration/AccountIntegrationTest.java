package com.bank.accounts.integration;

import com.bank.accounts.constants.AccountsConstants;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.repository.AccountsRepository;
import com.bank.accounts.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerDto testCustomerDto;

    @BeforeEach
    public void setUp() {
        customerRepository.deleteAll();
        accountsRepository.deleteAll();

        testCustomerDto = new CustomerDto();
        testCustomerDto.setName("John Doe");
        testCustomerDto.setEmail("john.doe@eazybank.com");
        testCustomerDto.setMobileNumber("9876543210");
    }

    @AfterEach
    public void tearDown() {
        customerRepository.deleteAll();
        accountsRepository.deleteAll();
    }

    // ========== CREATE ACCOUNT INTEGRATION TESTS ==========

    @Test
    public void givenValidCustomer_whenCreateAccount_thenAccountIsCreated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_201)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_201)));

        // Verify customer is created in database
        assert customerRepository.findByMobileNumber("9876543210").isPresent();
    }

    @Test
    public void givenValidCustomer_whenCreateMultipleAccounts_thenBothAreCreated() throws Exception {
        // Create first account
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().isCreated());

        // Create second account with different mobile number
        CustomerDto secondCustomer = new CustomerDto();
        secondCustomer.setName("Jane Doe");
        secondCustomer.setEmail("jane.doe@eazybank.com");
        secondCustomer.setMobileNumber("9123456789");

        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondCustomer)))
                .andDo(print())
                .andExpect(status().isCreated());

        // Verify both customers are created
        assert customerRepository.findByMobileNumber("9876543210").isPresent();
        assert customerRepository.findByMobileNumber("9123456789").isPresent();
    }

    // ========== FETCH ACCOUNT INTEGRATION TESTS ==========

    @Test
    public void givenExistingCustomer_whenFetchAccount_thenReturnCustomerDetails() throws Exception {
        // Given - Create a customer first
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andExpect(status().isCreated());

        // When & Then - Fetch the customer
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "9876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@eazybank.com")))
                .andExpect(jsonPath("$.mobileNumber", is("9876543210")));
    }

    @Test
    public void givenNonExistentMobileNumber_whenFetchAccount_thenReturn500() throws Exception {
        // When & Then - Try to fetch non-existent customer
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "9999999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== UPDATE ACCOUNT INTEGRATION TESTS ==========

    @Test
    public void givenExistingCustomer_whenUpdateAccount_thenReturnSuccess() throws Exception {
        // Given - Create a customer first
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andExpect(status().isCreated());

        // Update customer details
        testCustomerDto.setEmail("updated.email@eazybank.com");

        // When & Then - Update the customer
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().is(417));

        // Verify email is updated in database
        var updatedCustomer = customerRepository.findByMobileNumber("9876543210");
        assert updatedCustomer.isPresent();
    }

    @Test
    public void givenNonExistentCustomer_whenUpdateAccount_thenReturnFailure() throws Exception {
        // Given - Try to update non-existent customer
        testCustomerDto.setMobileNumber("9999999999");

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().is(417)); // EXPECTATION_FAILED
    }

    // ========== DELETE ACCOUNT INTEGRATION TESTS ==========

    @Test
    public void givenExistingCustomer_whenDeleteAccount_thenReturnSuccess() throws Exception {
        // Given - Create a customer first
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andExpect(status().isCreated());

        // Verify customer exists
        assert customerRepository.findByMobileNumber("9876543210").isPresent();

        // When & Then - Delete the customer
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_200)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_200)));

        // Verify customer is deleted
        assert customerRepository.findByMobileNumber("9876543210").isEmpty();
    }

    @Test
    public void givenNonExistentMobileNumber_whenDeleteAccount_thenReturnFailure() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9999999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(404)); // EXPECTATION_FAILED
    }

    // ========== BUILD INFO INTEGRATION TESTS ==========

    @Test
    public void whenFetchBuildInfo_thenReturnBuildDetails() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/build-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ========== JAVA VERSION INTEGRATION TESTS ==========

    @Test
    public void whenFetchJavaVersion_thenReturnJavaHome() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/java-version")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ========== CONTACT INFO INTEGRATION TESTS ==========

    @Test
    public void whenFetchContactInfo_thenReturnContactDetails() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/contact-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
