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
 *
 */

package org.eclipse.dataspaceconnector.provision.oauth2;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Generates {@link Oauth2ResourceDefinition}s for data addresses matching fields.
 */
public class Oauth2ConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    private final Predicate<DataAddress> validator = new Oauth2DataAddressValidator();

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        var destination = dataRequest.getDataDestination();
        if (!validator.test(destination)) {
            return null;
        }

        return Oauth2ResourceDefinition.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .transferProcessId(dataRequest.getProcessId())
            .dataAddress(destination)
            .build();
    }
}
