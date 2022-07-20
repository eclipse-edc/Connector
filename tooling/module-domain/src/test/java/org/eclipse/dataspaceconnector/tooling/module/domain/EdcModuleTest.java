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

package org.eclipse.dataspaceconnector.tooling.module.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EdcModuleTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var module = EdcModule.Builder.newInstance()
                .id("foo:bar")
                .version("1.0.0")
                .name("test")
                .overview("overview")
                .categories(List.of("category"))
                .extensionPoints(List.of(new Service("com.bar.BarService")))
                .provides(List.of(new Service("com.bar.BazService")))
                .references(List.of(new ServiceReference("com.bar.QuuxService", false)))
                .configuration(List.of(ConfigurationSetting.Builder.newInstance().key("key1").build()))
                .build();

        var serialized = mapper.writeValueAsString(module);
        var deserialized = mapper.readValue(serialized, EdcModule.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getCategories().size()).isEqualTo(1);
        assertThat(deserialized.getExtensionPoints().size()).isEqualTo(1);
        assertThat(deserialized.getProvides().size()).isEqualTo(1);
        assertThat(deserialized.getReferences().size()).isEqualTo(1);
        assertThat(deserialized.getConfiguration().size()).isEqualTo(1);
    }
}
