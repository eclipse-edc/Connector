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
        assertThat(res.getPortMappings()).hasSize(1).allSatisfy(pm -> {
            assertThat(pm.getName()).isEqualTo("default");
            assertThat(pm.getPort()).isEqualTo(1234);
        });
    }

    @Test
    void createFromConfig_noPortFound() {
        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of()));
        assertThat(res.getPortMappings()).hasSize(1).allSatisfy(pm -> {
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

        assertThat(res.getPortMappings()).hasSize(2).anySatisfy(pm -> {
            assertThat(pm.getName()).isEqualTo("default");
            assertThat(pm.getPort()).isEqualTo(1234);
            assertThat(pm.getPath()).isEqualTo("/api");
        }).anySatisfy(pm -> {
            assertThat(pm.getName()).isEqualTo("another");
            assertThat(pm.getPort()).isEqualTo(8888);
            assertThat(pm.getPath()).isEqualTo("/foo/bar");
        });
    }

    @Test
    void createFromConfig_explicitDefaultAndAnotherPort() {
        var res = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.default.port", "1234",
                "web.http.another.port", "8888",
                "web.http.another.path", "/foo/bar"
        )));

        assertThat(res.getPortMappings()).hasSize(2).anySatisfy(pm -> {
            assertThat(pm.getName()).isEqualTo("default");
            assertThat(pm.getPort()).isEqualTo(1234);
            assertThat(pm.getPath()).isEqualTo("/api");
        }).anySatisfy(pm -> {
            assertThat(pm.getName()).isEqualTo("another");
            assertThat(pm.getPort()).isEqualTo(8888);
            assertThat(pm.getPath()).isEqualTo("/foo/bar");
        });
    }

    @Test
    void createFromConfig_implicitAndExplicitDefault_shouldThrowException() {
        ThrowableAssert.ThrowingCallable doubleDefaultConfig = () -> JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.port", "1234",
                "web.http.default.port", "8888"
        )));

        assertThatThrownBy(doubleDefaultConfig).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("A port mapping for web.http.default.port already exists, currently mapped to {port=1234}");
    }

    @Test
    void createFromConfig_invalidAliasIsIgnored() {
        var result = JettyConfiguration.createFromConfig(null, null,
                ConfigFactory.fromMap(Map.of("web.http.this.is.longer.port", "8888")));

        assertThat(result.getPortMappings()).allSatisfy(p -> assertThat(p).usingRecursiveComparison().isEqualTo(new PortMapping()));

    }

    // The exception should be thrown when starting Jetty -> Servlet already exists
    @Test
    void createFromConfig_multipleContextsIdenticalPath_shouldNotThrowException() {
        var result = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.port", "8888",
                "web.http.path", "/foo",
                "web.http.another.port", "1234",
                "web.http.another.name", "test",
                "web.http.another.path", "/foo"
        )));
        assertThat(result.getPortMappings()).hasSize(2).allMatch(pm -> pm.getPath().equals("/foo"));


    }

    // The exception should be thrown when starting Jetty -> failed to bind to port
    @Test
    void createFromConfig_multipleContextsIdenticalPort_shouldNotThrowException() {
        var result = JettyConfiguration.createFromConfig(null, null, ConfigFactory.fromMap(Map.of(
                "web.http.port", "8888",
                "web.http.path", "/foo",
                "web.http.another.port", "8888",
                "web.http.another.name", "test",
                "web.http.another.path", "/another"
        )));
        assertThat(result.getPortMappings()).hasSize(2).allMatch(pm -> pm.getPort() == 8888);

    }
}