/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Improvements
 *
 */

package org.eclipse.edc.spi.system.configuration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ConfigFactoryTest {

    @Test
    void shouldConvertEnvironmentToPropertiesFormat() {
        var environment = Map.of("ENV_KEY_FORMAT", "value");

        var config = ConfigFactory.fromEnvironment(environment);

        assertThat(config).isNotNull().extracting(it -> it.getString("env.key.format")).isEqualTo("value");
    }
}
