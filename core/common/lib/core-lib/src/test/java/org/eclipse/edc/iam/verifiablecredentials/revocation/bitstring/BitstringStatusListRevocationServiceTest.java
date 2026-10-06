/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.TestData;
import org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.StatusMessage;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Collections.singleton;
import static org.eclipse.edc.iam.verifiablecredentials.TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_ARRAY_SUBJECT_TEMPLATE;
import static org.eclipse.edc.iam.verifiablecredentials.TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.BITSTRING_STATUSLIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_INDEX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_MESSAGES;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_PURPOSE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_SIZE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitstringStatusListRevocationServiceTest {

    private static final int REVOKED_INDEX = 10;
    private static final int NOT_REVOKED_INDEX = 15;

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final TokenValidationService tokenValidationService = mock(TokenValidationService.class);
    private final DidPublicKeyResolver didPublicKeyResolver = mock(DidPublicKeyResolver.class);

    private final BitstringStatusListRevocationService revocationService = new BitstringStatusListRevocationService(mapper,
            5 * 60 * 1000, singleton("application/json"), new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock()),
            tokenValidationService, didPublicKeyResolver);

    @BeforeEach
    void setUp() {
    }

    private String generateBitstring() {
        return generateBitstring(0, 0);
    }

    private String generateBitstring(int index, long value) {
        var bitstring = BitString.Builder.newInstance().size(1024 * 16).build();
        bitstring.set(index, value != 0);

        return BitString.Writer.newInstance().encoder(Base64.getUrlEncoder().withoutPadding()).writeMultibase(bitstring).getContent();
    }

    @Nested
    public class CheckValidity {
        @Test
        void checkValidity_revoked_notCached() {
            var bitstring = generateBitstring(REVOKED_INDEX, (byte) 0x2);
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);

            server.stubFor(get("/credentials/status/3").willReturn(ok(bitstringCredential)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
        }

        @Test
        void checkValidity_revoked_whenCached() {
            var bitstring = generateBitstring(REVOKED_INDEX, 1);
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);

            server.stubFor(get("/credentials/status/3").willReturn(ok(bitstringCredential)));


            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
            server.verify(getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @Test
        void checkValidity_notRevoked_notCached() {
            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            server.stubFor(get("/credentials/status/3").willReturn(ok(bitstringCredential)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
        }

        @Test
        void checkValidity_notRevoked_whenCached() {
            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            server.stubFor(get("/credentials/status/3").willReturn(ok(bitstringCredential)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
            server.verify(getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @Test
        void checkValidity_credentialPurposeMismatch_notCached() {
            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            server.stubFor(get("/credentials/status/3").willReturn(ok(bitstringCredential)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "suspension",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().startsWith("Credential's statusPurpose value must match the statusPurpose of the Bitstring Credential:");
        }

        @Test
        void checkValidity_whenSubjectIsArray_notRevoked() {
            var bitstring = generateBitstring();
            var testData = BITSTRING_STATUS_LIST_CREDENTIAL_ARRAY_SUBJECT_TEMPLATE.formatted(bitstring);
            server.stubFor(get("/credentials/status/3").willReturn(ok(testData)));


            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
        }

        @Test
        void checkValidity_whenSubjectIsArray_revoked() {
            var bitstring = generateBitstring(REVOKED_INDEX, 1);
            var testData = BITSTRING_STATUS_LIST_CREDENTIAL_ARRAY_SUBJECT_TEMPLATE.formatted(bitstring);
            server.stubFor(get("/credentials/status/3").willReturn(ok(testData)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
        }

        @Test
        void checkValidity_wrongContentType_expect415() {
            server.stubFor(get("/credentials/status/3").willReturn(aResponse().withStatus(415)));
            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .matches("Failed to download status list credential .* 415 Unsupported Media Type");
            // a client error is not retried
            server.verify(1, getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @Test
        void checkValidity_serverErrorThenSuccess_shouldRetry() {
            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            // the status list service is briefly unavailable, e.g. because it restarts
            server.stubFor(get("/credentials/status/3").inScenario("restart")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(503))
                    .willSetStateTo("available"));
            server.stubFor(get("/credentials/status/3").inScenario("restart")
                    .whenScenarioStateIs("available")
                    .willReturn(ok(bitstringCredential)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
            server.verify(2, getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @ParameterizedTest
        @ValueSource(ints = { 500, 502, 503, 504 })
        void checkValidity_serverErrorPersists_shouldFailAfterRetries(int status) {
            server.stubFor(get("/credentials/status/3").willReturn(aResponse().withStatus(status)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .matches("Failed to download status list credential from .*: " + status);
            // the initial attempt, plus the two retries of the default retry policy
            server.verify(3, getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @ParameterizedTest
        @ValueSource(ints = { 400, 401, 403, 404 })
        void checkValidity_clientError_shouldFailWithoutRetry(int status) {
            server.stubFor(get("/credentials/status/3").willReturn(aResponse().withStatus(status)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .matches("Failed to download status list credential from .*: " + status + " .*");
            server.verify(1, getRequestedFor(urlEqualTo("/credentials/status/3")));
        }

        @Test
        void checkValidity_connectionFailurePersists_shouldFailAfterRetries() {
            server.stubFor(get("/credentials/status/3").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .startsWith("Failed to download status list credential");
        }

        @Test
        void checkValidity_responseIsNoStatusListCredential_shouldFail() {
            // e.g. a proxy in front of the status list service answers with an HTML page
            server.stubFor(get("/credentials/status/3").willReturn(ok("<html>not a credential</html>")));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .startsWith("Failed to read status list credential from http://localhost:%d/credentials/status/3".formatted(server.getPort()));
            server.verify(1, getRequestedFor(urlEqualTo("/credentials/status/3")));
        }
    }

    @Nested
    public class GetStatusPurpose {

        @Test
        void getStatusPurpose_responseIsNoStatusListCredential_shouldFail() {
            server.stubFor(get("/credentials/status/3").willReturn(ok("<html>not a credential</html>")));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "revocation",
                                    STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isFailed()
                    .detail()
                    .startsWith("Failed to read status list credential");
        }

        @Test
        void getStatusPurpose_singleStatusSet() {

            var body = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1));
            server.stubFor(get("/credentials/status/3").willReturn(ok(body)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "revocation",
                                    STATUS_LIST_INDEX, REVOKED_INDEX,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation");
        }

        @Test
        void getStatusPurpose_singleStatusSet_message() {
            var body = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("message", generateBitstring(REVOKED_INDEX, 1));
            server.stubFor(get("/credentials/status/3").willReturn(ok(body)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "message",
                                    STATUS_LIST_INDEX, REVOKED_INDEX,
                                    STATUS_LIST_SIZE, 1,
                                    STATUS_LIST_MESSAGES, List.of(new StatusMessage("0x0", "accepted"), new StatusMessage("0x1", "rejected")),
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("rejected");
        }

        @Test
        void getStatusPurpose_singleStatusNotSet_message() {
            var body = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("message", generateBitstring());
            server.stubFor(get("/credentials/status/3").willReturn(ok(body)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "message",
                                    STATUS_LIST_INDEX, 69,
                                    STATUS_LIST_SIZE, 1,
                                    STATUS_LIST_MESSAGES, List.of(new StatusMessage("0x0", "accepted"), new StatusMessage("0x1", "rejected")),
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("accepted");
        }


        @Test
        void getStatusPurpose_singleStatus_notSet() {
            var body = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1));
            server.stubFor(get("/credentials/status/3").willReturn(ok(body)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "revocation",
                                    STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isNull();
        }

        @Test
        void getStatusPurpose_multipleStatus_onlyOneSet() {

            var first = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1));
            var second = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("suspension", generateBitstring());

            server.stubFor(get("/credentials/status/3").willReturn(ok(first)));
            server.stubFor(get("/credentials/status/4").willReturn(ok(second)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "revocation",
                                    STATUS_LIST_INDEX, REVOKED_INDEX,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "suspension",
                                    STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/4".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation");
        }

        @Test
        void getStatusPurpose_multipleCredentialStatus() {

            var first = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(42, 1));
            var second = BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("suspension", generateBitstring(69, 1));

            server.stubFor(get("/credentials/status/3").willReturn(ok(first)));
            server.stubFor(get("/credentials/status/4").willReturn(ok(second)));

            var credential = TestFunctions.createCredentialBuilder()
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "revocation",
                                    STATUS_LIST_INDEX, 42,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort()))))
                    .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                            Map.of(STATUS_LIST_PURPOSE, "suspension",
                                    STATUS_LIST_INDEX, 69,
                                    STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/4".formatted(server.getPort()))))
                    .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation, suspension");
        }
    }

    @Nested
    public class StatusListCredentialAsJwt {

        private final BitstringStatusListRevocationService acceptJwtService = new BitstringStatusListRevocationService(mapper, 5 * 60 * 1000,
                singleton("application/vc+jwt"), new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock()),
                tokenValidationService, didPublicKeyResolver);

        @Test
        void downloadStatusListCredential_asJwt_successfulTokenValidation() throws Exception {
            var jwtCredential = "eyABCDE";
            server.stubFor(get("/credentials/status/3").willReturn(ok(jwtCredential)));

            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            when(tokenValidationService.validate(eq(jwtCredential), eq(didPublicKeyResolver), any(List.class)))
                    .thenReturn(Result.success(ClaimToken.Builder.newInstance()
                            .claim("vc", mapper.readValue(bitstringCredential, LinkedTreeMap.class))
                            .build()));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));

            assertThat(acceptJwtService.checkValidity(credential)).isSucceeded();
            verify(tokenValidationService, times(1)).validate(eq(jwtCredential), eq(didPublicKeyResolver), any(List.class));
        }

        @Test
        void downloadStatusListCredential_asJwt_failureInTokenValidation() throws Exception {
            var jwtCredential = "eyABCDE";
            server.stubFor(get("/credentials/status/3").willReturn(ok(jwtCredential)));

            var bitstring = generateBitstring();
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            when(tokenValidationService.validate(eq(jwtCredential), eq(didPublicKeyResolver), any(List.class)))
                    .thenReturn(Result.failure("error validating JWT"));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(server.getPort())));

            assertThat(acceptJwtService.checkValidity(credential)).isFailed();
            verify(tokenValidationService, times(1)).validate(eq(jwtCredential), eq(didPublicKeyResolver), any(List.class));
        }
    }
}