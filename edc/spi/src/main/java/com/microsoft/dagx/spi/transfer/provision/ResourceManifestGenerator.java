/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.policy.model.AtomicConstraintFunction;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Prohibition;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

import java.util.Map;

/**
 * Generates resource manifests for data transfer requests. Implementations are responsible for enforcing policy contraints associated with transfer requests.
 */
public interface ResourceManifestGenerator {

    void registerClientGenerator(ResourceDefinitionGenerator generator);

    void registerProviderGenerator(ResourceDefinitionGenerator generator);

    void registerPermissionFunctions(Map<String, AtomicConstraintFunction<String, Permission, Boolean>> functions);

    void registerProhibitionFunctions(Map<String, AtomicConstraintFunction<String, Prohibition, Boolean>> functions);

    void registerObligationFunctions(Map<String, AtomicConstraintFunction<String, Duty, Boolean>> functions);

    /**
     * Generates a resource manifest for a data request on a client connector. Operations should be idempotent.
     */
    ResourceManifest generateClientManifest(TransferProcess process);

    /**
     * Generates a resource manifest for a data request on a provider connector. Operations should be idempotent.
     */
    ResourceManifest generateProviderManifest(TransferProcess process);

}
