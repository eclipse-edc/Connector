/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.edr;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.CONTRACT_ID;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;

public class DataAddressToEndpointDataReferenceTransformer implements TypeTransformer<DataAddress, EndpointDataReference> {

    private static final Set<String> PROPERTIES = Set.of(
            ID,
            CONTRACT_ID,
            ENDPOINT,
            AUTH_CODE,
            DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY,
            AUTH_KEY);

    @Override
    public Class<DataAddress> getInputType() {
        return DataAddress.class;
    }

    @Override
    public Class<EndpointDataReference> getOutputType() {
        return EndpointDataReference.class;
    }

    @Override
    public @Nullable EndpointDataReference transform(@NotNull DataAddress address, @NotNull TransformerContext context) {
        if (!address.getType().equals(EDR_SIMPLE_TYPE)) {
            context.reportProblem(format("Failed to convert data address with type %s to an EDR", address.getType()));
            return null;
        }

        var properties = address.getProperties().entrySet().stream()
                .filter(entry -> !PROPERTIES.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return EndpointDataReference.Builder.newInstance()
                .id(address.getStringProperty(ID))
                .contractId(address.getStringProperty(CONTRACT_ID))
                .authCode(address.getStringProperty(AUTH_CODE))
                .authKey(address.getStringProperty(AUTH_KEY))
                .endpoint(address.getStringProperty(ENDPOINT))
                .properties(properties)
                .build();
    }
}
