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

package org.eclipse.edc.iam.verifiablecredentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist2021.StatusList2021Credential.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist2021.StatusList2021Credential.STATUS_LIST_INDEX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist2021.StatusList2021Credential.STATUS_LIST_PURPOSE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;

class StatusListRevocationServiceTest {
    private static final int NOT_REVOKED_INDEX = 1;
    private static final int REVOKED_INDEX = 2;
    private final StatusListRevocationService revocationService = new StatusListRevocationService(new ObjectMapper().registerModule(new JavaTimeModule()),
            5 * 60 * 1000);
    private ClientAndServer clientAndServer;

    @BeforeEach
    void setup() {
        clientAndServer = ClientAndServer.startClientAndServer("localhost", getFreePort());
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_INTERMEDIATE));
    }

    @AfterEach
    void tearDown() {
        clientAndServer.stop();
    }

    @ParameterizedTest
    @ArgumentsSource(ArraySubjectProvider.class)
    void checkRevocation_whenSubjectIsArray(String testData) {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(testData));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
    }

    @Test
    void checkRevocation_whenNotCached_valid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
    }

    @Test
    void checkRevocation_whenNotCached_credentialPurposeMismatch() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "suspension",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isFailed()
                .detail().startsWith("Credential's statusPurpose value must match the status list's purpose:");
    }

    @Test
    void checkRevocation_whenNotCached_invalid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isFailed()
                .detail().isEqualTo("Credential status is 'revocation', status at index %d is '1'".formatted(REVOKED_INDEX));
    }

    @Test
    void checkRevocation_whenCached_valid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021Entry",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
        clientAndServer.verify(request(), VerificationTimes.exactly(1));
    }

    @ParameterizedTest
    @ArgumentsSource(SingleSubjectProvider.class)
    void getStatusPurposes_whenSingleCredentialStatusRevoked(String testData) {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(testData));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isEqualTo("revocation");
    }

    @ParameterizedTest
    @ArgumentsSource(ArraySubjectProvider.class)
    void getStatusPurposes_whenMultipleCredentialStatusRevoked(String testData) {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(testData));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isEqualTo("revocation");
    }

    @ParameterizedTest
    @ArgumentsSource(SingleSubjectProvider.class)
    void getStatusPurpose_whenCredentialStatusNotActive(String testData) {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(testData));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(SingleSubjectProvider.class)
    void getStatusPurpose_whenNoCredentialStatus(String testData) {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(testData));
        var credential = TestFunctions.createCredentialBuilder().build();
        assertThat(revocationService.getStatusPurpose(credential))
                .isNotNull()
                .isSucceeded();
    }


    private static class SingleSubjectProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(Named.of("VC (intermediate)", TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_INTERMEDIATE)),
                    Arguments.of(Named.of("VC 1.1", TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_1_0)),
                    Arguments.of(Named.of("VC 2.0", TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT_2_0))

            );
        }
    }

    private static class ArraySubjectProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(Named.of("VC 1.1", TestData.STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY_1_0)),
                    Arguments.of(Named.of("VC (intermediate)", TestData.STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY_INTERMEDIATE)),
                    Arguments.of(Named.of("VC 2.0", TestData.STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY_2_0))

            );
        }
    }
}