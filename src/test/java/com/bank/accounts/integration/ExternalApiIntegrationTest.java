package com.bank.accounts.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("External API Integration Tests with WireMock")
class ExternalApiIntegrationTest {

    private static WireMockServer wireMockServer;
    private WebClient webClient;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .dynamicPort()
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();
    }

    @Test
    @DisplayName("WireMock: GET Request Success")
    void testGetRequest_Success() {
        // Given
        String responseBody = "{\"accountNumber\":1000000001,\"accountType\":\"Savings\"}";
        
        stubFor(get(urlEqualTo("/api/accounts/1000000001"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Mono<String> response = webClient.get()
                .uri("/api/accounts/1000000001")
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> {
                    assertThat(body).contains("1000000001");
                    assertThat(body).contains("Savings");
                })
                .verifyComplete();

        verify(exactly(1), getRequestedFor(urlEqualTo("/api/accounts/1000000001")));
    }

    @ParameterizedTest
    @DisplayName("WireMock: GET Request with Multiple Account Numbers")
    @ValueSource(longs = {1000000001L, 1000000002L, 1000000003L})
    void testGetRequest_MultipleAccountNumbers(long accountNumber) {
        // Given
        String responseBody = String.format(
                "{\"accountNumber\":%d,\"accountType\":\"Savings\"}",
                accountNumber
        );
        
        stubFor(get(urlEqualTo("/api/accounts/" + accountNumber))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Mono<String> response = webClient.get()
                .uri("/api/accounts/" + accountNumber)
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> assertThat(body).contains(String.valueOf(accountNumber)))
                .verifyComplete();
    }

    @Test
    @DisplayName("WireMock: POST Request Success")
    void testPostRequest_Success() {
        // Given
        String requestBody = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";
        String responseBody = "{\"status\":\"created\",\"accountNumber\":1000000001}";
        
        stubFor(post(urlEqualTo("/api/accounts"))
                .withRequestBody(containing("John Doe"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Mono<String> response = webClient.post()
                .uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> {
                    assertThat(body).contains("created");
                    assertThat(body).contains("1000000001");
                })
                .verifyComplete();

        verify(exactly(1), postRequestedFor(urlEqualTo("/api/accounts"))
                .withRequestBody(containing("John Doe")));
    }

    @Test
    @DisplayName("WireMock: 404 Not Found Scenario")
    void testGetRequest_NotFound() {
        // Given
        stubFor(get(urlEqualTo("/api/accounts/9999999999"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"Account not found\"}")));

        // When
        Mono<String> response = webClient.get()
                .uri("/api/accounts/9999999999")
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        clientResponse -> Mono.error(new RuntimeException("Not Found")))
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> 
                        throwable instanceof RuntimeException && 
                        throwable.getMessage().contains("Not Found"))
                .verify();
    }

    @Test
    @DisplayName("WireMock: Timeout Scenario")
    void testGetRequest_Timeout() {
        // Given
        stubFor(get(urlEqualTo("/api/accounts/slow"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(3000) // 3 second delay
                        .withBody("{\"message\":\"slow response\"}")));

        // When
        Mono<String> response = webClient.get()
                .uri("/api/accounts/slow")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(1)); // 1 second timeout

        // Then
        StepVerifier.create(response)
                .expectError()
                .verify();
    }

    @RepeatedTest(value = 5, name = "Load Test - Repetition {currentRepetition}")
    @DisplayName("WireMock: Repeated Load Test")
    void testRepeatedRequests(RepetitionInfo repetitionInfo) {
        // Given
        String endpoint = "/api/accounts/load-test-" + repetitionInfo.getCurrentRepetition();
        stubFor(get(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("{\"iteration\":" + repetitionInfo.getCurrentRepetition() + "}")));

        // When
        Mono<String> response = webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> assertThat(body)
                        .contains(String.valueOf(repetitionInfo.getCurrentRepetition())))
                .verifyComplete();
    }

    @Test
    @DisplayName("WireMock: PUT Request Update Success")
    void testPutRequest_Success() {
        // Given
        String requestBody = "{\"accountType\":\"Current\",\"branchAddress\":\"New Branch\"}";
        String responseBody = "{\"status\":\"updated\",\"accountNumber\":1000000001}";
        
        stubFor(put(urlEqualTo("/api/accounts/1000000001"))
                .withRequestBody(containing("Current"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Mono<String> response = webClient.put()
                .uri("/api/accounts/1000000001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> assertThat(body).contains("updated"))
                .verifyComplete();

        verify(exactly(1), putRequestedFor(urlEqualTo("/api/accounts/1000000001")));
    }

    @Test
    @DisplayName("WireMock: DELETE Request Success")
    void testDeleteRequest_Success() {
        // Given
        stubFor(delete(urlEqualTo("/api/accounts/1000000001"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("{\"status\":\"deleted\"}")));

        // When
        Mono<String> response = webClient.delete()
                .uri("/api/accounts/1000000001")
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> assertThat(body).contains("deleted"))
                .verifyComplete();

        verify(exactly(1), deleteRequestedFor(urlEqualTo("/api/accounts/1000000001")));
    }

    @Test
    @DisplayName("WireMock: Request with Custom Headers")
    void testRequestWithHeaders() {
        // Given
        stubFor(get(urlEqualTo("/api/accounts/secure"))
                .withHeader("Authorization", equalTo("Bearer token123"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("{\"secure\":\"data\"}")));

        // When
        Mono<String> response = webClient.get()
                .uri("/api/accounts/secure")
                .header("Authorization", "Bearer token123")
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(response)
                .assertNext(body -> assertThat(body).contains("secure"))
                .verifyComplete();

        verify(exactly(1), getRequestedFor(urlEqualTo("/api/accounts/secure"))
                .withHeader("Authorization", equalTo("Bearer token123")));
    }
}