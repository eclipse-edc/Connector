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
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EndpointDataReferenceServiceRegistryImpl implements EndpointDataReferenceServiceRegistry {

    private final Map<String, EndpointDataReferenceService> registry = new HashMap<>();
    private final Map<String, EndpointDataReferenceService> responseChannelRegistry = new HashMap<>();

    @Override
    public void register(String addressType, EndpointDataReferenceService edrService) {
        registry.put(addressType, edrService);
    }

    @Override
    public void registerResponseChannel(String addressType, EndpointDataReferenceService edrService) {
        responseChannelRegistry.put(addressType, edrService);
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
    public ServiceResult<DataAddress> createResponseChannel(DataFlow dataFlow, DataAddress address) {
        var service = responseChannelRegistry.get(address.getType());
        if (service == null) {
            return ServiceResult.notFound("No EDR response channel service with type %s found".formatted(address.getType()));
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

    @Override
    public Set<String> supportedDestinationTypes() {
        return registry.keySet();
    }

    @Override
    public Set<String> supportedResponseTypes() {
        return responseChannelRegistry.keySet();
    }
}
