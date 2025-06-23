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
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.iam.verifiablecredentials.TestData;
import org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.StatusMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.util.Base64;
import java.util.List;
import java.util.Map;

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
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;

class BitstringStatusListRevocationServiceTest {

    private static final int REVOKED_INDEX = 10;
    private static final int NOT_REVOKED_INDEX = 15;

    private final BitstringStatusListRevocationService revocationService = new BitstringStatusListRevocationService(new ObjectMapper().registerModule(new JavaTimeModule()),
            5 * 60 * 1000, singleton("application/vc+jwt"), new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock()));
    private ClientAndServer clientAndServer;

    @BeforeEach
    void setUp() {
        clientAndServer = ClientAndServer.startClientAndServer("localhost", getFreePort());
        var bitstring = generateBitstring();
        var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(bitstringCredential));
    }

    @AfterEach
    void tearDown() {
        clientAndServer.stop();
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
            clientAndServer.reset()
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(bitstringCredential));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
        }

        @Test
        void checkValidity_revoked_whenCached() {
            var bitstring = generateBitstring(REVOKED_INDEX, 1);
            var bitstringCredential = TestData.BitstringStatusList.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_TEMPLATE.formatted(bitstring);
            clientAndServer.reset()
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(bitstringCredential));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
            clientAndServer.verify(request(), VerificationTimes.exactly(1));
        }

        @Test
        void checkValidity_notRevoked_notCached() {
            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
        }

        @Test
        void checkValidity_notRevoked_whenCached() {
            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
            clientAndServer.verify(request(), VerificationTimes.exactly(1));
        }

        @Test
        void checkValidity_credentialPurposeMismatch_notCached() {
            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "suspension",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().startsWith("Credential's statusPurpose value must match the statusPurpose of the Bitstring Credential:");
        }

        @Test
        void checkValidity_whenSubjectIsArray_notRevoked() {
            var bitstring = generateBitstring();
            var testData = BITSTRING_STATUS_LIST_CREDENTIAL_ARRAY_SUBJECT_TEMPLATE.formatted(bitstring);
            clientAndServer.reset();
            clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(testData));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isSucceeded();
        }

        @Test
        void checkValidity_whenSubjectIsArray_revoked() {
            var bitstring = generateBitstring(REVOKED_INDEX, 1);
            var testData = BITSTRING_STATUS_LIST_CREDENTIAL_ARRAY_SUBJECT_TEMPLATE.formatted(bitstring);
            clientAndServer.reset();
            clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(testData));

            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail().isEqualTo("Credential status is 'revocation', status at index 10 is '1'");
        }

        @Test
        void checkValidity_wrongContentType_expect415() {
            clientAndServer.reset()
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(415));
            var credential = new CredentialStatus("test-id", BITSTRING_STATUSLIST_CREDENTIAL,
                    Map.of(STATUS_LIST_PURPOSE, "revocation",
                            STATUS_LIST_INDEX, REVOKED_INDEX,
                            STATUS_LIST_SIZE, 1,
                            STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
            assertThat(revocationService.checkValidity(credential)).isFailed()
                    .detail()
                    .matches("Failed to download status list credential .* 415 Unsupported Media Type");
        }
    }

    @Nested
    public class GetStatusPurpose {

        @Test
        void getStatusPurpose_singleStatusSet() {
            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1))));
            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "revocation",
                                                     STATUS_LIST_INDEX, REVOKED_INDEX,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation");
        }

        @Test
        void getStatusPurpose_singleStatusSet_message() {
            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("message", generateBitstring(REVOKED_INDEX, 1))));
            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "message",
                                                     STATUS_LIST_INDEX, REVOKED_INDEX,
                                                     STATUS_LIST_SIZE, 1,
                                                     STATUS_LIST_MESSAGES, List.of(new StatusMessage("0x0", "accepted"), new StatusMessage("0x1", "rejected")),
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("rejected");
        }

        @Test
        void getStatusPurpose_singleStatusNotSet_message() {
            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("message", generateBitstring())));
            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "message",
                                                     STATUS_LIST_INDEX, 69,
                                                     STATUS_LIST_SIZE, 1,
                                                     STATUS_LIST_MESSAGES, List.of(new StatusMessage("0x0", "accepted"), new StatusMessage("0x1", "rejected")),
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("accepted");
        }


        @Test
        void getStatusPurpose_singleStatus_notSet() {
            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1))));
            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "revocation",
                                                     STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isNull();
        }

        @Test
        void getStatusPurpose_multipleStatus_onlyOneSet() {
            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(REVOKED_INDEX, 1))));
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/4"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("suspension", generateBitstring())));

            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "revocation",
                                                     STATUS_LIST_INDEX, REVOKED_INDEX,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "suspension",
                                                     STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/4".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation");
        }

        @Test
        void getStatusPurpose_multipleCredentialStatus() {

            clientAndServer.reset();
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/3"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("revocation", generateBitstring(42, 1))));
            clientAndServer
                    .when(request().withMethod("GET").withPath("/credentials/status/4"))
                    .respond(HttpResponse.response().withStatusCode(200).withBody(BITSTRING_STATUS_LIST_CREDENTIAL_PURPOSE_TEMPLATE.formatted("suspension", generateBitstring(69, 1))));

            var credential = TestFunctions.createCredentialBuilder()
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "revocation",
                                                     STATUS_LIST_INDEX, 42,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                                     .credentialStatus(new CredentialStatus("test-id", BitstringStatusListStatus.TYPE,
                                             Map.of(STATUS_LIST_PURPOSE, "suspension",
                                                     STATUS_LIST_INDEX, 69,
                                                     STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/4".formatted(clientAndServer.getPort()))))
                                     .build();
            assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                    .isEqualTo("revocation, suspension");
        }
    }
}