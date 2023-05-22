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
 *
 */

package org.eclipse.edc.connector.defaults.callback;

import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CallbackRegistryImplTest {

    CallbackRegistryImpl callbackRegistry = new CallbackRegistryImpl();

    @Test
    void resolve_shouldReturnsMatchedCallbacks() {

        var cb1 = CallbackAddress.Builder.newInstance()
                .uri("http://url1")
                .transactional(false)
                .events(Set.of("transfer", "contract"))
                .build();
        var cb2 = CallbackAddress.Builder.newInstance()
                .uri("http://url2")
                .transactional(false)
                .events(Set.of("asset", "policy"))
                .authCodeId("codeId")
                .authKey("key")
                .build();

        var cb3 = CallbackAddress.Builder.newInstance()
                .uri("http://url3")
                .transactional(true)
                .events(Set.of("transfer"))
                .build();

        Stream.of(cb1, cb2, cb3).forEach(callbackRegistry::register);

        assertThat(callbackRegistry.resolve("transfer"))
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("events")
                .containsExactly(cb1, cb3);

        assertThat(callbackRegistry.resolve("asset"))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("events")
                .containsExactly(cb2)
                .first()
                .satisfies(callbackAddress -> assertThat(callbackAddress.getEvents()).containsExactlyInAnyOrder("asset", "policy"));
    }
}
