/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.common.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationFunctionsTest {

    public static final String DEFAULT = "default";
    public static final String ENV_VAR_1 = "EDC_KEY1";
    public static final String ENV_VAR_2 = "EDC_KEY2";
    public static final String EDC_VAR_3 = "EDC_KEY3";
    private static final String SYS_PROP_1 = "edc.key1";
    private static final String SYS_PROP_2 = "edc.key2";
    private static final String SYS_PROP_3 = "edc.key3";
    public static final String VALUE_1 = "value1";

    @AfterEach
    @ClearEnvironmentVariable(key = ENV_VAR_1)
    @ClearEnvironmentVariable(key = ENV_VAR_2)
    @ClearEnvironmentVariable(key = EDC_VAR_3)
    @ClearSystemProperty(key = SYS_PROP_1)
    @ClearSystemProperty(key = SYS_PROP_2)
    @ClearSystemProperty(key = SYS_PROP_3)
    public void cleanUp() {
        // clear env vars and system properties
    }

    @ParameterizedTest
    @MethodSource("propertiesSource")
    @SetSystemProperty(key = SYS_PROP_1, value = VALUE_1)
    @SetSystemProperty(key = SYS_PROP_2, value = "")
    @SetSystemProperty(key = SYS_PROP_3, value = "    ")
    public void returnSystemProperty(String key, String expected) {
        String resultValue = ConfigurationFunctions.propOrEnv(key, DEFAULT);
        assertThat(resultValue).isEqualTo(expected);
    }

    @ParameterizedTest
    @SetEnvironmentVariable(key = ENV_VAR_1, value = VALUE_1)
    @SetEnvironmentVariable(key = ENV_VAR_2, value = "")
    @SetEnvironmentVariable(key = EDC_VAR_3, value = "    ")
    @MethodSource("envVarsSource")
    public void returnEnv(String key, String expected) {
        String resultValue = ConfigurationFunctions.propOrEnv(key, DEFAULT);
        assertThat(resultValue).isEqualTo(expected);
    }

    @Test
    public void returnDefaultEnv_NullValue() {
        String resultValue = ConfigurationFunctions.propOrEnv("nonexistent", DEFAULT);
        assertThat(resultValue).isEqualTo(DEFAULT);
    }

    private static Stream<Arguments> propertiesSource() {
        return Stream.of(
                Arguments.of(SYS_PROP_1, VALUE_1),
                Arguments.of(SYS_PROP_2, DEFAULT),
                Arguments.of(SYS_PROP_3, DEFAULT)
        );
    }

    private static Stream<Arguments> envVarsSource() {
        return Stream.of(
                Arguments.of(ENV_VAR_1, VALUE_1),
                Arguments.of(SYS_PROP_1, VALUE_1),
                Arguments.of(ENV_VAR_2, DEFAULT),
                Arguments.of(EDC_VAR_3, DEFAULT)
        );
    }
}