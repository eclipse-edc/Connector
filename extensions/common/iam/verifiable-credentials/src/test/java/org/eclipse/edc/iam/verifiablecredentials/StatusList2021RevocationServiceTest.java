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
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist.StatusList2021Credential.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist.StatusList2021Credential.STATUS_LIST_INDEX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist.StatusList2021Credential.STATUS_LIST_PURPOSE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;

class StatusList2021RevocationServiceTest {
    private static final int NOT_REVOKED_INDEX = 1;
    private static final int REVOKED_INDEX = 2;
    private final StatusList2021RevocationService revocationService = new StatusList2021RevocationService(new ObjectMapper().registerModule(new JavaTimeModule()),
            5 * 60 * 1000);
    private ClientAndServer clientAndServer;

    @BeforeEach
    void setup() {
        clientAndServer = ClientAndServer.startClientAndServer("localhost", getFreePort());
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT));
    }

    @AfterEach
    void tearDown() {
        clientAndServer.stop();
    }

    @Test
    void checkRevocation_whenSubjectIsArray() {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY));
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

    @Test
    void getStatusPurposes_whenSingleCredentialStatusRevoked() {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isEqualTo("revocation");
    }

    @Test
    void getStatusPurposes_whenMultipleCredentialStatusRevoked() {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isEqualTo("revocation");
    }

    @Test
    void getStatusPurpose_whenCredentialStatusNotActive() {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT));
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of(STATUS_LIST_PURPOSE, "revocation",
                                STATUS_LIST_INDEX, NOT_REVOKED_INDEX,
                                STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.getStatusPurpose(credential)).isSucceeded()
                .isNull();
    }

    @Test
    void getStatusPurpose_whenNoCredentialStatus() {
        clientAndServer.reset();
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(TestData.STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT));
        var credential = TestFunctions.createCredentialBuilder().build();
        assertThat(revocationService.getStatusPurpose(credential))
                .isSucceeded()
                .isNull(); //content is null, not the result!
    }

}