/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.asset.spi.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class DataplaneMetadataTest {

    @Test
    void serdes() throws JsonProcessingException {
        var mapper = new JacksonTypeManager().getMapper();

        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance()
                .property("key", "value")
                .label("gold")
                .build();

        var json = mapper.writeValueAsString(dataplaneMetadata);
        var deserialized = mapper.readValue(json, DataplaneMetadata.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dataplaneMetadata);
        assertThat(deserialized.getProperties()).containsExactly(entry("key", "value"));
        assertThat(deserialized.getLabels()).containsExactly("gold");
    }
}
