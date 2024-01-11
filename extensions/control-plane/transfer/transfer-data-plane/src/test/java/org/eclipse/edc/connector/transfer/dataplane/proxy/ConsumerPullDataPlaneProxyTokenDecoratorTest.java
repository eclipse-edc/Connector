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

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;

class ConsumerPullDataPlaneProxyTokenDecoratorTest {

    private Date expiration;
    private String encryptedDataAddress;

    private ConsumerPullDataPlaneProxyTokenDecorator decorator;

    @BeforeEach
    public void setUp() {
        expiration = Date.from(Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(1, 10)));
        encryptedDataAddress = UUID.randomUUID().toString();
        decorator = new ConsumerPullDataPlaneProxyTokenDecorator(expiration, encryptedDataAddress);
    }

    @Test
    void verifyDecorate() {

        var headers = new HashMap<String, Object>();
        var claims = new HashMap<String, Object>();
        var b = TokenParameters.Builder.newInstance();
        decorator.decorate(b);


        assertThat(b.build().getHeaders()).isEmpty();
        assertThat(b.build().getAdditional())
                .containsEntry(DATA_ADDRESS, encryptedDataAddress)
                .containsEntry(EXPIRATION_TIME, expiration);
    }
}