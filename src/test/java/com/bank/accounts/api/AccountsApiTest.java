package com.bank.accounts.api;

import com.bank.accounts.constants.AccountsConstants;
import com.bank.accounts.dto.AccountsDto;
import com.bank.accounts.dto.ContactInfoDto;
import com.bank.accounts.dto.CustomerDto;
import com.bank.accounts.service.IAccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "build.info=v1-test",
        "JAVA_HOME=/usr/lib/jvm/java-21"
})
@DisplayName("AccountsApi Integration Tests")
class AccountsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAccountsService accountsService;

    @MockitoBean
    private ContactInfoDto contactInfoDto;

    private CustomerDto customerDto;
    private AccountsDto accountsDto;

    @BeforeEach
    void setUp() {
        accountsDto = AccountsDto.builder()
                .accountNumber(1000000001L)
                .accountType("Savings")
                .branchAddress("123 Main St")
                .build();

        customerDto = CustomerDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("9876543210")
                .accountsDto(accountsDto)
                .build();
    }

    @Test
    @DisplayName("POST /api/create - Create Account Success")
    void testCreateAccount_Success() throws Exception {
        // Given
        doNothing().when(accountsService).createAccount(any(CustomerDto.class));

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is("201")))
                .andExpect(jsonPath("$.statusMsg", is("Account created successfully")));

        verify(accountsService, times(1)).createAccount(any(CustomerDto.class));
    }

    @ParameterizedTest
    @DisplayName("POST /api/create - Invalid Name Validation")
    @ValueSource(strings = {"", "AB", "A", "ThisIsAVeryLongNameThatExceedsThirtyCharactersLimit"})
    void testCreateAccount_InvalidName(String invalidName) throws Exception {
        // Given
        customerDto.setName(invalidName);

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).createAccount(any(CustomerDto.class));
    }

    @ParameterizedTest
    @DisplayName("POST /api/create - Invalid Email Validation")
    @ValueSource(strings = {"", "invalid-email", "@example.com", "test@", "test.com"})
    void testCreateAccount_InvalidEmail(String invalidEmail) throws Exception {
        // Given
        customerDto.setEmail(invalidEmail);

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).createAccount(any(CustomerDto.class));
    }

    @ParameterizedTest
    @DisplayName("POST /api/create - Invalid Mobile Number Validation")
    @ValueSource(strings = {"", "123", "12345678901", "abcdefghij", "987-654-3210"})
    void testCreateAccount_InvalidMobileNumber(String invalidMobile) throws Exception {
        // Given
        customerDto.setMobileNumber(invalidMobile);

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).createAccount(any(CustomerDto.class));
    }

    @ParameterizedTest
    @DisplayName("POST /api/create - Multiple Valid Customer Creation")
    @CsvSource({
            "Alice Johnson, alice@example.com, 9111111111",
            "Bob Smith, bob@example.com, 9222222222",
            "Charlie Brown, charlie@example.com, 9333333333"
    })
    void testCreateAccount_MultipleValidCustomers(String name, String email, String mobileNumber) throws Exception {
        // Given
        CustomerDto dto = CustomerDto.builder()
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .build();
        doNothing().when(accountsService).createAccount(any(CustomerDto.class));

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(accountsService, times(1)).createAccount(any(CustomerDto.class));
    }

    @RepeatedTest(value = 5, name = "Create Account - Load Test {currentRepetition}/{totalRepetitions}")
    @DisplayName("POST /api/create - Repeated Load Test")
    void testCreateAccount_LoadTest() throws Exception {
        // Given
        doNothing().when(accountsService).createAccount(any(CustomerDto.class));

        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isCreated());

        verify(accountsService, times(1)).createAccount(any(CustomerDto.class));
        reset(accountsService); // Reset for next iteration
    }

    // ==================== FETCH ACCOUNT TESTS ====================

    @Test
    @DisplayName("GET /api/fetch - Fetch Account Success")
    void testFetchAccount_Success() throws Exception {
        // Given
        String mobileNumber = "9876543210";
        when(accountsService.fetchAccount(anyString())).thenReturn(customerDto);

        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", mobileNumber))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.mobileNumber", is(mobileNumber)))
                .andExpect(jsonPath("$.accountsDto.accountNumber", is(1000000001)))
                .andExpect(jsonPath("$.accountsDto.accountType", is("Savings")));

        verify(accountsService, times(1)).fetchAccount(mobileNumber);
    }

    @ParameterizedTest
    @DisplayName("GET /api/fetch - Multiple Valid Mobile Numbers")
    @CsvSource({
            "9876543210, John Doe, john@example.com",
            "9111111111, Alice Johnson, alice@example.com",
            "9222222222, Bob Smith, bob@example.com"
    })
    void testFetchAccount_MultipleValidNumbers(String mobileNumber, String name, String email) throws Exception {
        // Given
        CustomerDto dto = CustomerDto.builder()
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .accountsDto(accountsDto)
                .build();
        when(accountsService.fetchAccount(mobileNumber)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.mobileNumber", is(mobileNumber)));

        verify(accountsService, times(1)).fetchAccount(mobileNumber);
    }

    @ParameterizedTest
    @DisplayName("GET /api/fetch - Invalid Mobile Number Validation")
    @ValueSource(strings = {"123", "12345678901", "abcdefghij"})
    void testFetchAccount_InvalidMobileNumber(String invalidMobile) throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", invalidMobile))
                .andExpect(status().isInternalServerError());

        verify(accountsService, never()).fetchAccount(anyString());
    }

    @RepeatedTest(value = 3, name = "Fetch Account - Repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("GET /api/fetch - Repeated Test")
    void testFetchAccount_RepeatedTest() throws Exception {
        // Given
        String mobileNumber = "9876543210";
        when(accountsService.fetchAccount(anyString())).thenReturn(customerDto);

        // When & Then
        mockMvc.perform(get("/api/fetch")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobileNumber", is(mobileNumber)));

        verify(accountsService, times(1)).fetchAccount(mobileNumber);
        reset(accountsService);
    }

    // ==================== UPDATE ACCOUNT TESTS ====================

    @Test
    @DisplayName("PUT /api/update - Update Account Success")
    void testUpdateAccount_Success() throws Exception {
        // Given
        when(accountsService.updateAccount(any(CustomerDto.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_200)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_200)));

        verify(accountsService, times(1)).updateAccount(any(CustomerDto.class));
    }

    @Test
    @DisplayName("PUT /api/update - Update Account Failure")
    void testUpdateAccount_Failure() throws Exception {
        // Given
        when(accountsService.updateAccount(any(CustomerDto.class))).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_417)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_417_UPDATE)));

        verify(accountsService, times(1)).updateAccount(any(CustomerDto.class));
    }

    @ParameterizedTest
    @DisplayName("PUT /api/update - Different Account Types")
    @CsvSource({
            "Savings, 123 Main St",
            "Current, 456 Oak Ave",
            "Fixed Deposit, 789 Pine Rd"
    })
    void testUpdateAccount_DifferentAccountTypes(String accountType, String branchAddress) throws Exception {
        // Given
        accountsDto.setAccountType(accountType);
        accountsDto.setBranchAddress(branchAddress);
        customerDto.setAccountsDto(accountsDto);
        when(accountsService.updateAccount(any(CustomerDto.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isOk());

        verify(accountsService, times(1)).updateAccount(any(CustomerDto.class));
    }

    @RepeatedTest(value = 4, name = "Update Account - Repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("PUT /api/update - Repeated Test")
    void testUpdateAccount_RepeatedTest() throws Exception {
        // Given
        when(accountsService.updateAccount(any(CustomerDto.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto)))
                .andExpect(status().isOk());

        verify(accountsService, times(1)).updateAccount(any(CustomerDto.class));
        reset(accountsService);
    }

    // ==================== DELETE ACCOUNT TESTS ====================

    @Test
    @DisplayName("DELETE /api/delete - Delete Account Success")
    void testDeleteAccount_Success() throws Exception {
        // Given
        String mobileNumber = "9876543210";
        when(accountsService.deleteAccount(anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", mobileNumber))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_200)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_200)));

        verify(accountsService, times(1)).deleteAccount(mobileNumber);
    }

    @Test
    @DisplayName("DELETE /api/delete - Delete Account Failure")
    void testDeleteAccount_Failure() throws Exception {
        // Given
        String mobileNumber = "9876543210";
        when(accountsService.deleteAccount(anyString())).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode", is(AccountsConstants.STATUS_417)))
                .andExpect(jsonPath("$.statusMsg", is(AccountsConstants.MESSAGE_417_DELETE)));

        verify(accountsService, times(1)).deleteAccount(mobileNumber);
    }

    @ParameterizedTest
    @DisplayName("DELETE /api/delete - Multiple Mobile Numbers")
    @ValueSource(strings = {"9876543210", "9111111111", "9222222222", "9333333333"})
    void testDeleteAccount_MultipleMobileNumbers(String mobileNumber) throws Exception {
        // Given
        when(accountsService.deleteAccount(mobileNumber)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk());

        verify(accountsService, times(1)).deleteAccount(mobileNumber);
    }

    @ParameterizedTest
    @DisplayName("DELETE /api/delete - Invalid Mobile Number Validation")
    @ValueSource(strings = {"123", "12345678901", "abcdefghij"})
    void testDeleteAccount_InvalidMobileNumber(String invalidMobile) throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", invalidMobile))
                .andExpect(status().isInternalServerError());

        verify(accountsService, never()).deleteAccount(anyString());
    }

    @RepeatedTest(value = 5, name = "Delete Account - Repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("DELETE /api/delete - Repeated Load Test")
    void testDeleteAccount_RepeatedTest() throws Exception {
        // Given
        String mobileNumber = "9876543210";
        when(accountsService.deleteAccount(anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/delete")
                        .param("mobileNumber", mobileNumber))
                .andExpect(status().isOk());

        verify(accountsService, times(1)).deleteAccount(mobileNumber);
        reset(accountsService);
    }

    // ==================== BUILD INFO TESTS ====================

    @Test
    @DisplayName("GET /api/build-info - Fetch Build Information")
    void testFetchBuildInfo_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/build-info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("v1-test"));
    }

    @RepeatedTest(value = 3, name = "Build Info - Repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("GET /api/build-info - Repeated Test")
    void testFetchBuildInfo_RepeatedTest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/build-info"))
                .andExpect(status().isOk())
                .andExpect(content().string("v1-test"));
    }

    // ==================== JAVA VERSION TESTS ====================


    @Test
    @DisplayName("GET /api/java-version - Fetch Java Version")
    void testFetchJavaVersion_Success() throws Exception {
        // When & Then - No mocking needed, uses real Environment bean
        mockMvc.perform(get("/api/java-version"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(notNullValue()));

        // No verification needed - Environment is not mocked
    }

    @Test
    @DisplayName("GET /api/java-version - Environment Property")
    void testFetchJavaVersion_WithProperty() throws Exception {
        // When & Then - Test uses property from @TestPropertySource
        mockMvc.perform(get("/api/java-version"))
                .andExpect(status().isOk());

        // Note: JAVA_HOME value comes from test properties or system environment
    }

    // ==================== CONTACT INFO TESTS ====================

    @Test
    @DisplayName("GET /api/contact-info - Fetch Contact Information")
    void testFetchContactInfo_Success() throws Exception {
        // Given
        ContactInfoDto mockContactInfo = new ContactInfoDto();
        mockContactInfo.setMessage("Welcome to Bank accounts related local APIs");

        ContactInfoDto.ContactDetails contactDetails = new ContactInfoDto.ContactDetails();
        contactDetails.setName("Chandan - Developer");
        contactDetails.setEmail("chandan03@zohomail.in");
        mockContactInfo.setContactDetails(contactDetails);

        mockContactInfo.setOnCallSupport(Arrays.asList("(555) 555-1234", "(555) 523-1345"));

        when(contactInfoDto.getMessage()).thenReturn(mockContactInfo.getMessage());
        when(contactInfoDto.getContactDetails()).thenReturn(mockContactInfo.getContactDetails());
        when(contactInfoDto.getOnCallSupport()).thenReturn(mockContactInfo.getOnCallSupport());

        // When & Then
        mockMvc.perform(get("/api/contact-info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Welcome to Bank accounts related local APIs")))
                .andExpect(jsonPath("$.contactDetails.name", is("Chandan - Developer")))
                .andExpect(jsonPath("$.contactDetails.email", is("chandan03@zohomail.in")))
                .andExpect(jsonPath("$.onCallSupport", hasSize(2)))
                .andExpect(jsonPath("$.onCallSupport[0]", is("(555) 555-1234")))
                .andExpect(jsonPath("$.onCallSupport[1]", is("(555) 523-1345")));

        verify(contactInfoDto, atLeastOnce()).getMessage();
        verify(contactInfoDto, atLeastOnce()).getContactDetails();
        verify(contactInfoDto, atLeastOnce()).getOnCallSupport();
    }

    @RepeatedTest(value = 2, name = "Contact Info - Repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("GET /api/contact-info - Repeated Test")
    void testFetchContactInfo_RepeatedTest() throws Exception {
        // Given
        when(contactInfoDto.getMessage()).thenReturn("Test message");

        // When & Then
        mockMvc.perform(get("/api/contact-info"))
                .andExpect(status().isOk());

        reset(contactInfoDto);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("POST /api/create - Empty Request Body")
    void testCreateAccount_EmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).createAccount(any(CustomerDto.class));
    }

    @Test
    @DisplayName("POST /api/create - Malformed JSON")
    void testCreateAccount_MalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).createAccount(any(CustomerDto.class));
    }

    @Test
    @DisplayName("GET /api/fetch - Missing Required Parameter")
    void testFetchAccount_MissingParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/fetch"))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).fetchAccount(anyString());
    }

    @Test
    @DisplayName("DELETE /api/delete - Missing Required Parameter")
    void testDeleteAccount_MissingParameter() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/delete"))
                .andExpect(status().isBadRequest());

        verify(accountsService, never()).deleteAccount(anyString());
    }
}
