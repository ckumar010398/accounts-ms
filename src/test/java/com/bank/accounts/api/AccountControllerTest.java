package com.bank.accounts.api;

import com.bank.accounts.constants.AccountsConstants;
import com.bank.accounts.dto.ContactInfoDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.service.IAccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IAccountsService iAccountsService;

    @Mock
    private Environment environment;

    @Mock
    private ContactInfoDto contactInfoDto;

    private AccountsApi accountsApi;
    private ObjectMapper objectMapper;
    private CustomerDto testCustomerDto;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        accountsApi = new AccountsApi();
        accountsApi.iAccountsService = iAccountsService;
        accountsApi.environment = environment;
        accountsApi.contactInfoDto = contactInfoDto;
        accountsApi.buildInfo = "Accounts Service - Build 1.0.0";

        mockMvc = MockMvcBuilders.standaloneSetup(accountsApi).build();

        // Initialize test customer DTO
        testCustomerDto = new CustomerDto();
        testCustomerDto.setName("John Doe");
        testCustomerDto.setEmail("john.doe@eazybank.com");
        testCustomerDto.setMobileNumber("9876543210");
    }

    // ========== CREATE ACCOUNT TESTS ==========

    @Test
    public void givenValidCustomerDto_whenCreateAccount_thenReturn201() throws Exception {
        // Given
        willDoNothing().given(iAccountsService).createAccount(any(CustomerDto.class));

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_201)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_201)));
    }

    @Test
    public void givenInvalidCustomerDto_whenCreateAccount_thenReturn400() throws Exception {
        // Given - Invalid DTO with null required fields
        CustomerDto invalidDto = new CustomerDto();
        invalidDto.setName(null);
        invalidDto.setEmail(null);
        invalidDto.setMobileNumber(null);

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenCustomerDtoWithInvalidEmail_whenCreateAccount_thenReturn400() throws Exception {
        // Given - Invalid email format
        testCustomerDto.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== FETCH ACCOUNT TESTS ==========

    @Test
    public void givenValidMobileNumber_whenFetchAccount_thenReturn200() throws Exception {
        // Given
        given(iAccountsService.fetchAccount(anyString())).willReturn(testCustomerDto);

        // When & Then
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
    public void givenInvalidMobileNumber_whenFetchAccount_thenReturn400() throws Exception {
        // When & Then - Mobile number with invalid format (9 digits instead of 10)
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "987654321")  // Invalid: only 9 digits
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenEmptyMobileNumber_whenFetchAccount_thenReturn200() throws Exception {
        // Given - Empty mobile number is allowed per regex
        given(iAccountsService.fetchAccount(anyString())).willReturn(testCustomerDto);

        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void givenMobileNumberWithAlphabets_whenFetchAccount_thenReturn400() throws Exception {
        // When & Then - Mobile number with alphabets should fail
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", "98765abc1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE ACCOUNT TESTS ==========

    @Test
    public void givenValidCustomerDto_whenUpdateAccount_thenReturn200() throws Exception {
        // Given
        given(iAccountsService.updateAccount(any(CustomerDto.class))).willReturn(true);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_200)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_200)));
    }

    @Test
    public void givenValidCustomerDto_whenUpdateAccountFails_thenReturn417() throws Exception {
        // Given
        given(iAccountsService.updateAccount(any(CustomerDto.class))).willReturn(false);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCustomerDto)))
                .andDo(print())
                .andExpect(status().is(417))  // EXPECTATION_FAILED
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_417)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_417_UPDATE)));
    }

    @Test
    public void givenInvalidCustomerDto_whenUpdateAccount_thenReturn400() throws Exception {
        // Given - Invalid DTO with null required fields
        CustomerDto invalidDto = new CustomerDto();

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE ACCOUNT TESTS ==========

    @Test
    public void givenValidMobileNumber_whenDeleteAccount_thenReturn200() throws Exception {
        // Given
        given(iAccountsService.deleteAccount(anyString())).willReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_200)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_200)));
    }

    @Test
    public void givenValidMobileNumber_whenDeleteAccountFails_thenReturn417() throws Exception {
        // Given
        given(iAccountsService.deleteAccount(anyString())).willReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(417))  // EXPECTATION_FAILED
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_417)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_417_DELETE)));
    }

    @Test
    public void givenInvalidMobileNumber_whenDeleteAccount_thenReturn400() throws Exception {
        // When & Then - Mobile number with invalid format
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "123")  // Invalid: less than 10 digits
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(417));
    }

    @Test
    public void givenMobileNumberWithSpecialCharacters_whenDeleteAccount_thenReturn400() throws Exception {
        // When & Then - Mobile number with special characters
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", "9876-543210")  // Invalid: contains hyphen
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(417));
    }

    // ========== BUILD INFO TESTS ==========

    @Test
    public void whenFetchBuildInfo_thenReturn200() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/build-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Accounts Service - Build 1.0.0"));
    }

    // ========== JAVA VERSION TESTS ==========

    @Test
    public void whenFetchJavaVersion_thenReturn200() throws Exception {
        // Given
        given(environment.getProperty("JAVA_HOME")).willReturn("/usr/lib/jvm/java-17-openjdk");

        // When & Then
        mockMvc.perform(get("/api/java-version")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("/usr/lib/jvm/java-17-openjdk"));
    }

    @Test
    public void whenFetchJavaVersion_withNullProperty_thenReturn200() throws Exception {
        // Given
        given(environment.getProperty("JAVA_HOME")).willReturn(null);

        // When & Then
        mockMvc.perform(get("/api/java-version")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
