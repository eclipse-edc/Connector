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
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;

public class EndpointDataReferenceServiceRegistryImpl implements EndpointDataReferenceServiceRegistry {

    private final Map<String, DataPlaneAuthorizationService> registry = new HashMap<>();

    @Override
    public void register(String addressType, DataPlaneAuthorizationService edrService) {
        registry.put(addressType, edrService);
    }

    @Override
    public ServiceResult<DataAddress> create(DataFlow dataFlow, DataAddress address) {
        var service = registry.get(address.getType());
        if (service == null) {
            return ServiceResult.notFound("No EDR service with type %s found".formatted(address.getType()));
        }

        return service.createEndpointDataReference(dataFlow).flatMap(ServiceResult::from);
    }

    @Override
    public ServiceResult<Void> revoke(DataFlow dataFlow, String reason) {
        var type = dataFlow.getTransferType().destinationType();
        var service = registry.get(type);
        if (service == null) {
            return ServiceResult.notFound("No EDR service with type %s found".formatted(type));
        }

        return service.revokeEndpointDataReference(dataFlow.getId(), reason);
    }
}
