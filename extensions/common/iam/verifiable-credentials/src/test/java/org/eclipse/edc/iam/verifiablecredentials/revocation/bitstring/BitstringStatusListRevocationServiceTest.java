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
import org.eclipse.edc.iam.verifiablecredentials.TestData;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.Base64;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_INDEX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_PURPOSE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_SIZE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;

class BitstringStatusListRevocationServiceTest {

    private static final int REVOKED_INDEX = 10;
    private static final int NOT_REVOKED_INDEX = 15;

    private final BitstringStatusListRevocationService revocationService = new BitstringStatusListRevocationService(new ObjectMapper().registerModule(new JavaTimeModule()),
            5 * 60 * 1000);
    private ClientAndServer clientAndServer;

    @BeforeEach
    void setUp() {
        clientAndServer = ClientAndServer.startClientAndServer("localhost", getFreePort());
        var bitstring = generateBitstring(REVOKED_INDEX, (byte) 0x2);
        var bitstringCredential = TestData.BITSTRING_STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT.formatted(bitstring);
        clientAndServer.when(request().withMethod("GET").withPath("/credentials/status/3"))
                .respond(HttpResponse.response().withStatusCode(200).withBody(bitstringCredential));
    }


    @AfterEach
    void tearDown() {
        clientAndServer.stop();
    }

    @Test
    void checkValidity_revoked_notCached() {
        var credential = new CredentialStatus("test-id", "StatusList2021",
                Map.of(STATUS_LIST_PURPOSE, "revocation",
                        STATUS_LIST_INDEX, REVOKED_INDEX,
                        STATUS_LIST_SIZE, 1,
                        STATUS_LIST_CREDENTIAL, "http://localhost:%d/credentials/status/3".formatted(clientAndServer.getPort())));
        assertThat(revocationService.checkValidity(credential)).isSucceeded();
    }

    private String generateBitstring(int index, byte value) {

        var bitstring = BitString.Builder.newInstance().size(1024 * 16).build();
        bitstring.setValue(index, value);

        return "u" + BitString.Writer.newInstance().encoder(Base64.getUrlEncoder().withoutPadding()).write(bitstring).getContent();
    }
}