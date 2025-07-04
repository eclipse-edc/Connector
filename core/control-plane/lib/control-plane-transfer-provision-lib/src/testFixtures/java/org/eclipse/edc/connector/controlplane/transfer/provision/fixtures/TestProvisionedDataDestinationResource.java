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

package org.eclipse.edc.connector.controlplane.transfer.provision.fixtures;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;

public class TestProvisionedDataDestinationResource extends ProvisionedDataDestinationResource {
    public TestProvisionedDataDestinationResource(String resourceName, String id) {
        this.resourceName = resourceName;
        this.id = id;
    }
}
