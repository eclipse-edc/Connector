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

package org.eclipse.dataspaceconnector.transfer.core.edr;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

import java.util.Map;

public class EndpointDataReferenceFixtures {

    private static final Faker FAKER = new Faker();

    public static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint(FAKER.internet().url())
                .authKey(FAKER.lorem().word())
                .authCode(FAKER.internet().uuid())
                .id(FAKER.internet().uuid())
                .properties(Map.of(FAKER.lorem().word(), FAKER.internet().uuid()))
                .build();
    }
}
