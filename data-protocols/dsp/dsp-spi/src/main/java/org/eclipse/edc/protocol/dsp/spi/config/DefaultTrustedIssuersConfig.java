/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;

import java.util.List;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

@Settings
public record DefaultTrustedIssuersConfig(

        @Setting(
                key = "id",
                description = "ID of the issuer."
        )
        String id,

        @Setting(
                key = "supportedtypes",
                description = "Supported credential types for this issuer, as a JSON serialized list.",
                defaultValue = "[\"*\"]")
        String supportedTypes

) {

    public static final String CONFIG_PREFIX = "edc.iam.trustedissuer";

    public TrustedIssuer createTrustedIssuer(ObjectMapper objectMapper) {
        return TrustedIssuer.Builder.newInstance()
                .id(id)
                .supportedTypes(deserializeSupportedTypes(objectMapper))
                .build();
    }

    private List<String> deserializeSupportedTypes(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(supportedTypes(), defaultInstance().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            throw new EdcException("Cannot deserialize TrustedIssuer supportedtypes json setting: '%s'".formatted(supportedTypes), e);
        }
    }

}
