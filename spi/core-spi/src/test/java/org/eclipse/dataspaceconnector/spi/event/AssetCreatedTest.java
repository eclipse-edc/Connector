/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class AssetCreatedTest {

    @Test
    void serdes() {
        var typeManager = new TypeManager();
        typeManager.registerTypes(new NamedType(AssetCreated.class, AssetCreated.class.getSimpleName()));

        var event = AssetCreated.Builder.newInstance()
                .id("id")
                .at(Clock.systemUTC().millis())
                .build();

        var json = typeManager.writeValueAsString(event);
        var deserialized = typeManager.readValue(json, Event.class);

        assertThat(deserialized)
                .isInstanceOf(AssetCreated.class)
                .usingRecursiveComparison().isEqualTo(event);
    }
}