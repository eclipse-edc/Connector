/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class PublicEndpointGeneratorServiceImpl implements PublicEndpointGeneratorService {
    private final Map<String, Function<DataAddress, Endpoint>> generatorFunctions = new ConcurrentHashMap<>();

    @Override
    public Result<Endpoint> generateFor(DataAddress sourceDataAddress) {
        Objects.requireNonNull(sourceDataAddress);
        Objects.requireNonNull(sourceDataAddress.getType());

        return Optional.ofNullable(generatorFunctions.get(sourceDataAddress.getType()))
                .map(function -> function.apply(sourceDataAddress))
                .map(Result::success)
                .orElseGet(() -> Result.failure("No Endpoint generator function registered for source data type '%s'".formatted(sourceDataAddress.getType())));
    }

    @Override
    public void addGeneratorFunction(String destinationType, Function<DataAddress, Endpoint> generatorFunction) {
        generatorFunctions.put(destinationType, generatorFunction);
    }
}
