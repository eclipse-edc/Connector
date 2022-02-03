/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.extension.jetty;

import org.assertj.core.api.ThrowableAssert;
import org.eclipse.dataspaceconnector.core.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JettyConfigurationTest {


    @Test
    void createFromConfig_defaultPort() {

        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of("web.http.port", "1234")));
        assertThat(res.getPortMappings()).hasSize(1).allSatisfy((s, pm) -> {
            assertThat(s).isEqualTo("default");
            assertThat(pm.getPort()).isEqualTo(1234);
        });
    }

    @Test
    void createFromConfig_noPortFound() {
        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of()));
        assertThat(res.getPortMappings()).hasSize(1).allSatisfy((s, pm) -> {
            assertThat(pm.getName()).isEqualTo("default");
            assertThat(pm.getPath()).isEqualTo("/api");
            assertThat(pm.getPort()).isEqualTo(8181);
        });

    }

    @Test
    void createFromConfig_implicitDefaultAndAnotherPort() {
        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.port", "1234",
                "web.http.another.port", "8888",
                "web.http.another.path", "/foo/bar"
        )));

        assertThat(res.getPortMappings()).hasSize(2).anySatisfy((s, pm) -> {
            assertThat(pm.getName()).isEqualTo("default");
            assertThat(pm.getPort()).isEqualTo(1234);
            assertThat(pm.getPath()).isEqualTo("/api");
        }).anySatisfy((s, pm) -> {
            assertThat(pm.getName()).isEqualTo("another");
            assertThat(pm.getPort()).isEqualTo(8888);
            assertThat(pm.getPath()).isEqualTo("/foo/bar");
        });
    }

    @Test
    void createFromConfig_implicitAndExplicitDefault_shouldOverwrite() {
        ThrowableAssert.ThrowingCallable doubleDefaultConfig = () -> JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.port", "1234",
                "web.http.default.port", "8888"
        )));

        assertThatThrownBy(doubleDefaultConfig).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("A default configuration was already specified");
    }

    @Test
    void createFromConfig_multiPathConfig() {
        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.this.is.longer.port", "8888"
        )));
        assertThat(res.getPortMappings()).hasSize(1).allSatisfy((s, pm) -> {
            assertThat(s).isEqualTo("this.is.longer");
            assertThat(pm.getPort()).isEqualTo(8888);
        });

    }
}