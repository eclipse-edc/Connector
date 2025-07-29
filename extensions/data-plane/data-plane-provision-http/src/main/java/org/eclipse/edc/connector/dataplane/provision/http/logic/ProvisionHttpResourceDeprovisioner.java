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

package org.eclipse.edc.connector.dataplane.provision.http.logic;

import org.eclipse.edc.connector.dataplane.provision.http.port.ProvisionHttpClient;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttp.PROVISION_HTTP_TYPE;

public class ProvisionHttpResourceDeprovisioner implements Deprovisioner {

    private final ProvisionHttpClient provisionHttpClient;

    public ProvisionHttpResourceDeprovisioner(ProvisionHttpClient provisionHttpClient) {
        this.provisionHttpClient = provisionHttpClient;
    }

    @Override
    public String supportedType() {
        return PROVISION_HTTP_TYPE;
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResource provisionResource) {
        return CompletableFuture.completedFuture(provisionHttpClient.deprovision(provisionResource));
    }
}
