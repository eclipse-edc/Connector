/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy contraints associated with transfer requests.
 */
@Feature("edc:core:transfer:provision:resourcemanifest-generator")
public interface ResourceManifestGenerator {

    void registerConsumerGenerator(ResourceDefinitionGenerator generator);

    void registerProviderGenerator(ResourceDefinitionGenerator generator);

    /**
     * Generates a resource manifest for a data request on a connector. Operations should be idempotent.
     */
    ResourceManifest generateResourceManifest(TransferProcess process);
}
