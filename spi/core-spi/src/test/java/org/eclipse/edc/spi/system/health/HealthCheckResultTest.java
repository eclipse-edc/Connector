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

package org.eclipse.edc.spi.system.health;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNotSerializeFailureMessagesAndDetail() {
        var result = HealthCheckResult.Builder.newInstance().component("test").failure("error").build();

        var json = objectMapper.readTree(objectMapper.writeValueAsString(new HealthStatus(result)));

        assertThat(json.get("isSystemHealthy").asBoolean()).isFalse();
        var componentResult = json.get("componentResults").get(0);
        assertThat(componentResult.get("isHealthy").asBoolean()).isFalse();
        assertThat(componentResult.get("component").asString()).isEqualTo("test");
        assertThat(componentResult.get("failure").get("messages").get(0).asString()).isEqualTo("error");
        assertThat(componentResult.has("failureMessages")).isFalse();
        assertThat(componentResult.has("failureDetail")).isFalse();
    }

}
