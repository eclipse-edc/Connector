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

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.Map;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy contraints associated with transfer requests.
 */
@Feature("edc:core:transfer:provision:resourcemanifest-generator")
public interface ResourceManifestGenerator {

    void registerConsumerGenerator(ResourceDefinitionGenerator generator);

    void registerProviderGenerator(ResourceDefinitionGenerator generator);

    void registerPermissionFunctions(Map<String, AtomicConstraintFunction<String, Permission, Boolean>> functions);

    void registerProhibitionFunctions(Map<String, AtomicConstraintFunction<String, Prohibition, Boolean>> functions);

    void registerObligationFunctions(Map<String, AtomicConstraintFunction<String, Duty, Boolean>> functions);

    /**
     * Generates a resource manifest for a data request on a consumer connector. Operations should be idempotent.
     */
    ResourceManifest generateConsumerManifest(TransferProcess process);

    /**
     * Generates a resource manifest for a data request on a provider connector. Operations should be idempotent.
     */
    ResourceManifest generateProviderManifest(TransferProcess process);

}
