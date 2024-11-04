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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderMethodScannerTest {

    private ProviderMethodScanner scanner;

    @BeforeEach
    void setup() {
        scanner = new ProviderMethodScanner(new TestExtension());
    }

    @Test
    void allProviders() throws NoSuchMethodException {
        assertThat(scanner.allProviders()).hasSize(3)
                .filteredOn(ProviderMethod::isDefault)
                .hasSize(1)
                .extracting(ProviderMethod::getMethod)
                .containsOnly(TestExtension.class.getMethod("providerDefault"));
    }

    @Test
    void verifyInvalidReturnType() {
        var scanner = new ProviderMethodScanner(new InvalidTestExtension());
        assertThatThrownBy(() -> scanner.allProviders().toList()).isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void verifyInvalidVisibility() {
        var scanner = new ProviderMethodScanner(new InvalidTestExtension2());

        assertThatThrownBy(() -> scanner.allProviders().toList()).isInstanceOf(EdcInjectionException.class);
    }

    private static class TestExtension implements ServiceExtension {
        public void someMethod() {

        }

        @Provider
        public Object providerMethodWithArg(ServiceExtensionContext context) {
            return new Object();
        }

        @Provider
        public String provider() {
            return "";
        }

        @Provider(isDefault = true)
        public String providerDefault() {
            return "";
        }
    }

    private static class InvalidTestExtension extends TestExtension {
        @Provider
        public void invalidProvider() {

        }
    }

    private static class InvalidTestExtension2 extends TestExtension {
        @Provider
        Object invalidProvider() {
            return new Object();
        }
    }

}
