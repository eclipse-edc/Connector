/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.sync.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.CONTRACT_ID;
import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.DATA_ADDRESS;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;

class DataPlaneProxyTokenDecoratorTest {

    private Date expiration;
    private String contractId;
    private String encryptedDataAddress;

    private DataPlaneProxyTokenDecorator decorator;

    @BeforeEach
    public void setUp() {
        expiration = Date.from(Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(1, 10)));
        contractId = UUID.randomUUID().toString();
        encryptedDataAddress = UUID.randomUUID().toString();
        decorator = new DataPlaneProxyTokenDecorator(expiration, contractId, encryptedDataAddress);
    }

    @Test
    void claims() {
        var result = decorator.claims();

        assertThat(result)
                .containsEntry(CONTRACT_ID, contractId)
                .containsEntry(DATA_ADDRESS, encryptedDataAddress)
                .containsEntry(EXPIRATION_TIME, expiration);
    }

    @Test
    void headers() {
        var result = decorator.headers();

        assertThat(result).isNotNull().isEmpty();
    }
}