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
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Constants for {@link EndpointDataReference} mapping to {@link DataAddress}
 */
public class EndpointDataAddressConstants {

    public static final String TYPE = "EDR";
    public static final String ID = "id";
    public static final String AUTH_CODE = "authCode";
    public static final String AUTH_KEY = "authKey";
    public static final String ENDPOINT = "endpoint";
    public static final String TYPE_FIELD = "type";

    private static final Set<String> PROPERTIES = Set.of(
            ID,
            EDC_NAMESPACE + ID,
            TYPE_FIELD,
            EDC_NAMESPACE + TYPE_FIELD,
            AUTH_CODE,
            EDC_NAMESPACE + AUTH_CODE,
            ENDPOINT,
            EDC_NAMESPACE + ENDPOINT,
            AUTH_KEY,
            EDC_NAMESPACE + AUTH_KEY);

    private EndpointDataAddressConstants() {
    }

    public static DataAddress from(EndpointDataReference edr) {
        return DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ID, edr.getId())
                .property(AUTH_CODE, edr.getAuthCode())
                .property(AUTH_KEY, edr.getAuthKey())
                .property(ENDPOINT, edr.getEndpoint())
                .properties(edr.getProperties())
                .build();
    }

    public static Result<EndpointDataReference> to(DataAddress address) {

        if (!address.getType().equals(TYPE)) {
            return Result.failure(format("Failed to convert data address with type %s to an EDR", address.getType()));
        }

        var properties = address.getProperties().entrySet().stream()
                .filter(entry -> !PROPERTIES.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var edr = EndpointDataReference.Builder.newInstance()
                .id(address.getProperty(ID))
                .authCode(address.getProperty(AUTH_CODE))
                .authKey(address.getProperty(AUTH_KEY))
                .endpoint(address.getProperty(ENDPOINT))
                .properties(properties)
                .build();

        return Result.success(edr);
    }
}
