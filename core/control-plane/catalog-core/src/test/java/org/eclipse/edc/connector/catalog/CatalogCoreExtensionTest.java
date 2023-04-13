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

package org.eclipse.edc.connector.catalog;

import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class CatalogCoreExtensionTest {

    private CatalogCoreExtension extension;

    @BeforeEach
    void setup(ObjectFactory factory) {
        extension = factory.constructInstance(CatalogCoreExtension.class);
    }

    @Test
    void shouldProvideServices(ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(context)
                .matches(c -> c.hasService(DatasetResolver.class))
                .matches(c -> c.hasService(DataServiceRegistry.class))
                .matches(c -> c.hasService(DistributionResolver.class));
    }
}
