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
 *       ZF Friedrichshafen AG - improvements (refactoring of canGenerate method)
 *
 */

package org.eclipse.edc.connector.provision.oauth2;

import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Generates {@link Oauth2ResourceDefinition}s for data addresses matching fields.
 */
public class Oauth2ConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    private final Predicate<DataAddress> validator = new Oauth2DataAddressValidator();

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        Objects.requireNonNull(dataRequest, "dataRequest must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        var destination = dataRequest.getDataDestination();

        return Oauth2ResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(dataRequest.getProcessId())
                .dataAddress(destination)
                .build();
    }

    @Override
    public boolean canGenerate(DataRequest dataRequest, Policy policy) {
        Objects.requireNonNull(dataRequest, "dataRequest must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        return validator.test(dataRequest.getDataDestination());
    }
}
