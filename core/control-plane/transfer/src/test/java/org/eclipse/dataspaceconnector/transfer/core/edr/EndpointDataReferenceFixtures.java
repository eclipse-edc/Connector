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

import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

import java.util.Map;
import java.util.UUID;

public class EndpointDataReferenceFixtures {


    public static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("test.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("test-key", UUID.randomUUID().toString()))
                .build();
    }
}
