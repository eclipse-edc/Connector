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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.Mockito.mock;

class DependencyGraphTest {

    private final DependencyGraph graph = new DependencyGraph(mock());

    @Test
    void sortExtensions_withDefaultProvider() {
        var providerExtension = TestFunctions.createProviderExtension(true);
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var list = graph.of(mutableListOf(dependentExtension, providerExtension));

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(providerExtension, dependentExtension);
    }

    @Test
    void sortExtensions_withNoDefaultProvider() {
        var defaultProvider = TestFunctions.createProviderExtension(false);
        var provider = TestFunctions.createProviderExtension(true);
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var list = graph.of(mutableListOf(dependentExtension, provider, defaultProvider));

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(provider, defaultProvider, dependentExtension);
    }

    @Test
    void sortExtensions_missingDependency() {
        var dependentExtension = TestFunctions.createDependentExtension(true);

        assertThatThrownBy(() -> graph.of(mutableListOf(dependentExtension)))
                .isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void sortExtensions_missingOptionalDependency() {
        var dependentExtension = TestFunctions.createDependentExtension(false);

        var injectionContainers = graph.of(mutableListOf(dependentExtension));

        assertThat(injectionContainers).hasSize(1)
                .extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(dependentExtension);
    }
}
