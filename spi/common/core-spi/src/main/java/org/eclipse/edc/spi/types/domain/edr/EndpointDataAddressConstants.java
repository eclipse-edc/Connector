/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.types.domain.edr;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.Builder;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;

/**
 * Constants for {@link EndpointDataReference} mapping to {@link DataAddress}
 */
public class EndpointDataAddressConstants {
    private static final Set<String> PROPERTIES = Set.of(
            ID,
            ENDPOINT,
            AUTH_CODE,
            DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY,
            AUTH_KEY);

    private EndpointDataAddressConstants() {
    }

    public static DataAddress from(EndpointDataReference edr) {
        return DataAddress.Builder.newInstance()
                .type(EDR_SIMPLE_TYPE)
                .property(ID, edr.getId())
                .property(AUTH_CODE, edr.getAuthCode())
                .property(AUTH_KEY, edr.getAuthKey())
                .property(ENDPOINT, edr.getEndpoint())
                .properties(edr.getProperties())
                .build();
    }

    public static Result<EndpointDataReference> to(DataAddress address) {

        if (!address.getType().equals(EDR_SIMPLE_TYPE)) {
            return Result.failure(format("Failed to convert data address with type %s to an EDR", address.getType()));
        }

        var properties = address.getProperties().entrySet().stream()
                .filter(entry -> !PROPERTIES.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var edr = Builder.newInstance()
                .id(address.getProperty(ID))
                .authCode(address.getProperty(AUTH_CODE))
                .authKey(address.getProperty(AUTH_KEY))
                .endpoint(address.getProperty(ENDPOINT))
                .properties(properties)
                .build();

        return Result.success(edr);
    }
}
