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

package org.eclipse.edc.connector.dataplane.selector.spi.instance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.REGISTERED;

class DataPlaneInstanceTest {

    private ObjectMapper mapper = new JacksonTypeManager().getMapper();

    @Test
    void verifySerialization() throws MalformedURLException, JsonProcessingException {
        var instance = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .lastActive(Instant.now().toEpochMilli())
                .url(new URL("http://localhost:8234/some/path"))
                .property("someprop", "someval")
                .allowedSourceType("allowedSrc1")
                .allowedSourceType("allowedSrc2")
                .authorizationProfile(new AuthorizationProfile("test-type", emptyMap()))
                .build();

        var json = mapper.writeValueAsString(instance);

        assertThat(json).isNotNull()
                .contains("url\":\"http://localhost:8234/some/path\"")
                .contains("\"someprop\":\"someval\"");

        var deserialized = mapper.readValue(json, DataPlaneInstance.class).copy();
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(instance);
    }

    @Test
    void copy_shouldCarryOverStateAndTimestamps() {
        var instance = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .createdAt(10)
                .updatedAt(20)
                .state(REGISTERED.code())
                .stateTimestamp(30)
                .url("http://localhost:8234/some/path")
                .build();

        var copy = instance.copy();

        assertThat(copy).usingRecursiveComparison().isEqualTo(instance);
        assertThat(copy.getId()).isEqualTo("test-id");
        assertThat(copy.getCreatedAt()).isEqualTo(10);
        assertThat(copy.getUpdatedAt()).isEqualTo(20);
        assertThat(copy.getState()).isEqualTo(REGISTERED.code());
        assertThat(copy.getStateTimestamp()).isEqualTo(30);
    }

    @Test
    void build_shouldDefaultTimestampsAndId() {
        var clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneId.of("UTC"));
        var instance = DataPlaneInstance.Builder.newInstance()
                .clock(clock)
                .url("http://localhost:8234/some/path")
                .build();

        assertThat(instance.getId()).isNotNull();
        assertThat(instance.getCreatedAt()).isEqualTo(clock.millis());
        assertThat(instance.getUpdatedAt()).isEqualTo(clock.millis());
        assertThat(instance.getStateTimestamp()).isEqualTo(clock.millis());
    }

    @Test
    void transition_shouldUpdateStateAndTimestamps() {
        var clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneId.of("UTC"));
        var instance = DataPlaneInstance.Builder.newInstance()
                .clock(clock)
                .createdAt(1)
                .updatedAt(1)
                .stateTimestamp(1)
                .url("http://localhost:8234/some/path")
                .build();

        instance.transitionToRegistered();

        assertThat(instance.getState()).isEqualTo(REGISTERED.code());
        assertThat(instance.getStateTimestamp()).isEqualTo(clock.millis());
        assertThat(instance.getUpdatedAt()).isEqualTo(clock.millis());
    }

}
