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

package org.eclipse.dataspaceconnector.boot.system;

import org.assertj.core.data.Index;
import org.eclipse.dataspaceconnector.spi.system.injection.EdcInjectionException;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.boot.system.TestFunctions.createDependentExtension;
import static org.eclipse.dataspaceconnector.boot.system.TestFunctions.createList;
import static org.eclipse.dataspaceconnector.boot.system.TestFunctions.createProviderExtension;

class DependencyGraphTest {

    private DependencyGraph sorter;

    @BeforeEach
    void setUp() {
        sorter = new DependencyGraph();
    }

    @Test
    void sortExtensions_withDefaultProvider() {
        var providerExtension = createProviderExtension(true);

        var dependentExtension = createDependentExtension(true);

        var list = sorter.of(createList(dependentExtension, providerExtension));
        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .contains(providerExtension, Index.atIndex(2))
                .contains(dependentExtension, Index.atIndex(3));

    }

    @Test
    void sortExtensions_withNoDefaultProvider() {
        var defaultProvider = createProviderExtension(false);
        var provider = createProviderExtension(true);
        var dependentExtension = createDependentExtension(true);

        var list = sorter.of(createList(dependentExtension, provider, defaultProvider));
        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .contains(provider, Index.atIndex(2))
                .contains(defaultProvider, Index.atIndex(3))
                .contains(dependentExtension, Index.atIndex(4));
    }

    @Test
    void sortExtensions_missingDependency() {

        var dependentExtension = createDependentExtension(true);
        assertThatThrownBy(() -> sorter.of(createList(dependentExtension))).isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void sortExtensions_missingOptionalDependency() {

        var dependentExtension = createDependentExtension(false);
        assertThat(sorter.of(createList(dependentExtension))).hasSize(3)
                .extracting(InjectionContainer::getInjectionTarget)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(dependentExtension);
    }

    @Test
    void sortExtensions_multipleDefaultProviders() {

    }


}