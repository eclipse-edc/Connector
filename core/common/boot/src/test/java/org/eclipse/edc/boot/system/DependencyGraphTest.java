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

import org.assertj.core.data.Index;
import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DependencyGraphTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph(mock(ServiceExtensionContext.class));
    }

    @Test
    void sortExtensions_withDefaultProvider() {
        var providerExtension = TestFunctions.createProviderExtension(true);

        var dependentExtension = TestFunctions.createDependentExtension(true);

        var list = graph.of(TestFunctions.createList(dependentExtension, providerExtension));
        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .contains(providerExtension, Index.atIndex(2))
                .contains(dependentExtension, Index.atIndex(3));

    }

    @Test
    void sortExtensions_withNoDefaultProvider() {
        var defaultProvider = TestFunctions.createProviderExtension(false);
        var provider = TestFunctions.createProviderExtension(true);
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var list = graph.of(TestFunctions.createList(dependentExtension, provider, defaultProvider));
        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .contains(provider, Index.atIndex(2))
                .contains(defaultProvider, Index.atIndex(3))
                .contains(dependentExtension, Index.atIndex(4));
    }

    @Test
    void sortExtensions_missingDependency() {

        var dependentExtension = TestFunctions.createDependentExtension(true);
        assertThatThrownBy(() -> graph.of(TestFunctions.createList(dependentExtension))).isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void sortExtensions_missingOptionalDependency() {

        var dependentExtension = TestFunctions.createDependentExtension(false);
        assertThat(graph.of(TestFunctions.createList(dependentExtension))).hasSize(3)
                .extracting(InjectionContainer::getInjectionTarget)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(dependentExtension);
    }
}