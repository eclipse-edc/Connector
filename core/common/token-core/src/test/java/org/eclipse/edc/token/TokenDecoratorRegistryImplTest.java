/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.token;

import org.eclipse.edc.token.spi.TokenDecorator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TokenDecoratorRegistryImplTest {

    private final TokenDecoratorRegistryImpl tokenDecoratorRegistry = new TokenDecoratorRegistryImpl();

    @Test
    void register_whenContextNotYetExists_shouldCreateEntry() {
        tokenDecoratorRegistry.register("test-context", (claims, headers) -> {
        });
        assertThat(tokenDecoratorRegistry.getDecoratorsFor("test-context")).hasSize(1);
    }

    @Test
    void register_whenContextExists() {
        tokenDecoratorRegistry.register("test-context", new TestTokenDecorator());
        tokenDecoratorRegistry.register("test-context", new TestTokenDecorator());

        assertThat(tokenDecoratorRegistry.getDecoratorsFor("test-context")).hasSize(2);
    }


    @Test
    void unregister_whenContextNotExist() {
        assertThatNoException().isThrownBy(() -> tokenDecoratorRegistry.unregister("not-exist", new TestTokenDecorator()));
    }

    @Test
    void unregister_whenContextExist() {
        TokenDecorator d1 = new TestTokenDecorator();
        TokenDecorator d2 = new TestTokenDecorator();

        tokenDecoratorRegistry.register("test-context", d1);
        assertThatNoException().isThrownBy(() -> tokenDecoratorRegistry.unregister("test-context", d2));
        assertThat(tokenDecoratorRegistry.getDecoratorsFor("not-exists")).isNotNull().isEmpty();

    }

    @Test
    void unregister_whenContextAndDecoratorExists() {
        TokenDecorator d1 = new TestTokenDecorator();
        TokenDecorator d2 = new TestTokenDecorator();

        tokenDecoratorRegistry.register("test-context", d1);
        tokenDecoratorRegistry.register("test-context", d2);
        assertThatNoException().isThrownBy(() -> tokenDecoratorRegistry.unregister("test-context", d2));
        assertThat(tokenDecoratorRegistry.getDecoratorsFor("test-context")).containsExactly(d1);
    }

    private static class TestTokenDecorator implements TokenDecorator {
        @Override
        public void decorate(Map<String, Object> claims, Map<String, Object> headers) {

        }
    }
}