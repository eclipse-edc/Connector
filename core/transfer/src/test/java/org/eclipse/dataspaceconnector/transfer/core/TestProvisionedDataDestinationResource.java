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
 *       Microsoft Corporation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;

public class TestProvisionedDataDestinationResource extends ProvisionedDataDestinationResource {
    private final String resourceName;

    public TestProvisionedDataDestinationResource(String resourceName, String id) {
        super();
        this.resourceName = resourceName;
        this.id = id;
    }

    @Override
    public DataAddress createDataDestination() {
        return null;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }
}
