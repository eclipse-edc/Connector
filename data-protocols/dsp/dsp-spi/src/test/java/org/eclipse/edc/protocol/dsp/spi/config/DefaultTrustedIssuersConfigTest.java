/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.protocol.dsp.spi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultTrustedIssuersConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateTrustedIssuer() {
        var config = new DefaultTrustedIssuersConfig("id", "[\"type1\", \"type2\"]");

        var result = config.createTrustedIssuer(objectMapper);

        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getSupportedTypes()).containsExactly("type1", "type2");
    }

    @Test
    void shouldThrowException_whenSupportedTypeIsNotValidJsonList() {
        var config = new DefaultTrustedIssuersConfig("id", "[\"type1\",]");

        assertThatThrownBy(() -> config.createTrustedIssuer(objectMapper)).isInstanceOf(EdcException.class);
    }
}