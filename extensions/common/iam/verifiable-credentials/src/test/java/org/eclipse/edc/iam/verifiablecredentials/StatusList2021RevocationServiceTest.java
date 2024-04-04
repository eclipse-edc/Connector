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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.TestData.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;

class StatusList2021RevocationServiceTest {
    private static final int NOT_REVOKED_INDEX = 42;
    private static final int REVOKED_INDEX = 359;
    private final StatusList2021RevocationService revocationService = new StatusList2021RevocationService(new ObjectMapper().registerModule(new JavaTimeModule()),
            5 * 60 * 1000);
    private ClientAndServer clientAndServer;

    @BeforeEach
    void setup() {
        clientAndServer = ClientAndServer.startClientAndServer("localhost", getFreePort());
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(STATUS_LIST_CREDENTIAL));
    }

    @Test
    void checkRevocation_whenNotCached_valid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of("statusPurpose", "revocation",
                                "statusListIndex", NOT_REVOKED_INDEX,
                                "statusListCredential", "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
    }

    @Test
    void checkRevocation_whenNotCached_credentialPurposeMismatch() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of("statusPurpose", "suspension",
                                "statusListIndex", NOT_REVOKED_INDEX,
                                "statusListCredential", "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isFailed()
                .detail().startsWith("Credential's statusPurpose value must match the status list's purpose:");
    }

    @Test
    void checkRevocation_whenNotCached_invalid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of("statusPurpose", "revocation",
                                "statusListIndex", REVOKED_INDEX,
                                "statusListCredential", "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isFailed()
                .detail().isEqualTo("Credential status is 'revocation', status at index %d is false".formatted(REVOKED_INDEX));
    }

    @Test
    void checkRevocation_whenCached_valid() {
        var credential = TestFunctions.createCredentialBuilder().credentialStatus(new CredentialStatus("test-id", "StatusList2021",
                        Map.of("statusPurpose", "revocation",
                                "statusListIndex", NOT_REVOKED_INDEX,
                                "statusListCredential", "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort()))))
                .build();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
        clientAndServer.verify(request(), VerificationTimes.exactly(1));
    }
}