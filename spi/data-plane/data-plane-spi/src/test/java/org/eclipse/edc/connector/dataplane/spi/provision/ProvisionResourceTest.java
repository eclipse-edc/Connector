/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.provision;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProvisionResourceTest {

    private final JacksonTypeManager typeManager = new JacksonTypeManager();

    @Test
    void serdes() throws JsonProcessingException {
        var resource = ProvisionResource.Builder.newInstance()
                .flowId(UUID.randomUUID().toString())
                .dataAddress(DataAddress.Builder.newInstance().type("any").build())
                .property("any", "any")
                .build();

        resource.transitionProvisioned(ProvisionedResource.Builder.from(resource).pending(true).dataAddress(DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build()).build());
        resource.transitionDeprovisioned(DeprovisionedResource.Builder.from(resource).pending(true).build());

        var json = typeManager.getMapper().writeValueAsString(resource);
        var deserialized = typeManager.getMapper().readValue(json, ProvisionResource.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(resource);
    }
}
