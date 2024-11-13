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
import org.eclipse.edc.boot.system.injection.ServiceInjectionPoint;
import org.eclipse.edc.boot.system.injection.ValueInjectionPoint;
import org.eclipse.edc.boot.system.testextensions.DependentExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void getDependenciesOf() {
        var providerExtension = createProviderExtension(false);
        var dependentExtension = createDependentExtension(true);

        var graph = DependencyGraph.of(mock(), List.of(providerExtension, dependentExtension));
        var dependencies = graph.getDependenciesOf(RequiredDependentExtension.class);
        assertThat(dependencies).hasSize(2)
                .anySatisfy(ip -> assertThat(ip).isInstanceOf(ServiceInjectionPoint.class))
                .anySatisfy(ip -> assertThat(ip).isInstanceOf(ValueInjectionPoint.class));
    }

    @Test
    void getDependentExtensions() {
        var providerExtension = createProviderExtension(true);
        var ext1 = createDependentExtension(true);
        var ext2 = createDependentExtension(false);

        var graph = DependencyGraph.of(mock(), List.of(providerExtension, ext1, ext2));
        var dependents = graph.getDependentExtensions(TestObject.class);

        assertThat(dependents).hasSize(2)
                .containsExactlyInAnyOrder(RequiredDependentExtension.class, DependentExtension.class);
    }

    @Test
    void getDependenciesFor() {
        var providerExtension = createProviderExtension(true);
        var ext1 = createDependentExtension(true);
        var ext2 = createDependentExtension(false);

        var graph = DependencyGraph.of(mock(), List.of(providerExtension, ext1, ext2));
        var deps = graph.getDependenciesFor(TestObject.class);
        assertThat(deps).hasSize(2);
    }

}
