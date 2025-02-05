/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

import static java.util.Optional.ofNullable;

public record CredentialStatus(String id, String type,
                               @JsonAnySetter @JsonAnyGetter Map<String, Object> additionalProperties) {
    public static final String CREDENTIAL_STATUS_ID_PROPERTY = "@id";
    public static final String CREDENTIAL_STATUS_TYPE_PROPERTY = "@type";


    /**
     * Returns a property if presents in the properties map. This method
     * will try first the combination namespace + property and if
     * not found will fall back to just property when fetching the property
     * from the underling map
     *
     * @param namespace The namespace of the property
     * @param property  The name of the property
     * @return The property if present, null otherwise
     */

    public Object getProperty(String namespace, String property) {
        return ofNullable(additionalProperties.get(namespace + property))
                .or(() -> ofNullable(additionalProperties.get(property)))
                .orElse(null);
    }

}
