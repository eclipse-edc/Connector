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

package org.eclipse.edc.connector.contract;

import org.eclipse.edc.connector.contract.policy.PolicyArchiveImpl;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class ContractCoreExtensionTest {

    private ContractCoreExtension extension;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(ExecutorInstrumentation.class, mock(ExecutorInstrumentation.class));
        extension = factory.constructInstance(ContractCoreExtension.class);
    }

    @Test
    void shouldProvidePolicyArchive(ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(context.hasService(PolicyArchive.class)).isTrue();
        assertThat(context.getService(PolicyArchive.class)).isNotNull().isInstanceOf(PolicyArchiveImpl.class);
    }
}
