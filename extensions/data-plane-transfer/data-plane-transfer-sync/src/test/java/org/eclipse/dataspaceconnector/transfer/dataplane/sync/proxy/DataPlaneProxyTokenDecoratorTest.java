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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy;

import com.github.javafaker.Faker;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.DATA_ADDRESS;

class DataPlaneProxyTokenDecoratorTest {
    private static final Faker FAKER = new Faker();

    private Date expiration;
    private String contractId;
    private String encryptedDataAddress;

    private DataPlaneProxyTokenDecorator decorator;

    @BeforeEach
    public void setUp() {
        expiration = Date.from(Instant.now().plusSeconds(FAKER.random().nextInt(1, 10)));
        contractId = FAKER.internet().uuid();
        encryptedDataAddress = FAKER.internet().uuid();
        decorator = new DataPlaneProxyTokenDecorator(expiration, contractId, encryptedDataAddress);
    }

    @Test
    void decorate() throws ParseException {
        var builder = new JWTClaimsSet.Builder();

        decorator.decorate(null, builder);

        var claims = builder.build();
        assertThat(claims.getStringClaim(CONTRACT_ID)).isEqualTo(contractId);
        assertThat(claims.getStringClaim(DATA_ADDRESS)).isEqualTo(encryptedDataAddress);
        assertThat(claims.getExpirationTime()).isNotNull()
                .isEqualTo(expiration);
    }
}