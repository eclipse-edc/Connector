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

import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.system.TestFunctions.createDependentExtension;
import static org.eclipse.edc.boot.system.TestFunctions.createProviderExtension;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.Mockito.mock;

class DependencyGraphTest {


    @Test
    void getExtensions_withDefaultProvider() {
        var providerExtension = createProviderExtension(true);
        var dependentExtension = createDependentExtension(true);

        var list = DependencyGraph.of(mock(), mutableListOf(dependentExtension, providerExtension)).getInjectionContainers();

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(providerExtension, dependentExtension);
    }

    @Test
    void getExtensions_withNoDefaultProvider() {
        var defaultProvider = createProviderExtension(false);
        var provider = createProviderExtension(true);
        var dependentExtension = createDependentExtension(true);

        var list = DependencyGraph.of(mock(), mutableListOf(dependentExtension, provider, defaultProvider)).getInjectionContainers();

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(provider, defaultProvider, dependentExtension);
    }

    @Test
    void getExtensions_missingDependency() {
        var dependentExtension = createDependentExtension(true);

        assertThat(DependencyGraph.of(mock(), mutableListOf(dependentExtension)).isValid()).isFalse();
    }

    @Test
    void getExtensions_missingOptionalDependency() {
        var dependentExtension = createDependentExtension(false);

        var dependencyGraph = DependencyGraph.of(mock(), mutableListOf(dependentExtension));

        assertThat(dependencyGraph.isValid()).isTrue();
        assertThat(dependencyGraph.getInjectionContainers()).hasSize(1)
                .extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(dependentExtension);
    }
}
