/*
 *  Copyright (c) 2024 Encho Belezirev (Digital Lights)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Encho Belezirev (Digital Lights) - refactor(test): improve QueryValidator testing strategy
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;

import java.util.List;
import java.util.Map;

public class TransferProcessServiceSubtypesProvider {
    public Map<Class<?>, List<Class<?>>> getSubtypes() {
        return Map.of(
                ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                ProvisionedDataAddressResource.class, List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)
        );
    }
}
