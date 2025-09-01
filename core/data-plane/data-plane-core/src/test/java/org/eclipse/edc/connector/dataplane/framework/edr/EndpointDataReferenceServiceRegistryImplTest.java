/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.edr;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceServiceRegistryImplTest {

    private final EndpointDataReferenceServiceRegistryImpl registry = new EndpointDataReferenceServiceRegistryImpl();

    @Nested
    class Create {
        @Test
        void shouldCreateEdrUsingRegisteredService() {
            var dataFlow = DataFlow.Builder.newInstance().build();
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            EndpointDataReferenceService service = mock();
            when(service.createEndpointDataReference(any())).thenReturn(Result.success(DataAddress.Builder.newInstance().type("EDR").build()));

            registry.register("type", service);

            var result = registry.create(dataFlow, dataAddress);

            assertThat(result).isSucceeded().extracting(DataAddress::getType).isEqualTo("EDR");
            verify(service).createEndpointDataReference(dataFlow);
        }

        @Test
        void shouldReturnNotFound_whenNoServiceAvailable() {
            var dataFlow = DataFlow.Builder.newInstance().build();
            var dataAddress = DataAddress.Builder.newInstance().type("unexisting type").build();

            var result = registry.create(dataFlow, dataAddress);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }

    }

    @Nested
    class Revoke {
        @Test
        void shouldRevokeEdrUsingRegisteredService() {
            var dataFlow = DataFlow.Builder.newInstance().transferType(new TransferType("type", FlowType.PULL)).build();
            EndpointDataReferenceService service = mock();
            var reason = "reason";
            when(service.revokeEndpointDataReference(any(), any())).thenReturn(ServiceResult.success());

            registry.register("type", service);

            var result = registry.revoke(dataFlow, reason);

            assertThat(result).isSucceeded();
            verify(service).revokeEndpointDataReference(dataFlow.getId(), reason);
        }

        @Test
        void shouldReturnNotFound_whenNoServiceAvailable() {
            var dataFlow = DataFlow.Builder.newInstance().transferType(new TransferType("type", FlowType.PULL)).build();

            var result = registry.revoke(dataFlow, "any");

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }
}


