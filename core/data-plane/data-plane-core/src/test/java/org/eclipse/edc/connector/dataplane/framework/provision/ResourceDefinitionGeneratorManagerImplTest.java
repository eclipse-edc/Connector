/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.provision;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceDefinition;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceDefinitionGeneratorManagerImplTest {

    private final ResourceDefinitionGeneratorManager manager = new ResourceDefinitionGeneratorManagerImpl();

    @Nested
    class Consumer {

        private final ResourceDefinitionGenerator supportedGenerator = mock();

        @BeforeEach
        void setUp() {
            when(supportedGenerator.supportedType()).thenReturn("supportedType");
            when(supportedGenerator.generate(any())).thenReturn(new ProvisionResourceDefinition());

            manager.registerConsumerGenerator(supportedGenerator);
        }

        @Test
        void generate_shouldGenerateResources() {
            var destination = DataAddress.Builder.newInstance().type("supportedType").build();
            var dataFlow = DataFlow.Builder.newInstance().destination(destination).build();

            var definitions = manager.generateConsumerResourceDefinition(dataFlow);

            assertThat(definitions).hasSize(1);
        }

        @Test
        void destinationTypes_shouldReturnRegisteredDestinationTypes() {
            var types = manager.destinationTypes();

            assertThat(types).containsOnly("supportedType");
        }
    }

}
