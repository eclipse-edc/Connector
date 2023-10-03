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

package org.eclipse.edc.iam.identitytrust.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.identitytrust.model.CredentialStatus;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.edc.identitytrust.model.CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY;
import static org.eclipse.edc.identitytrust.model.CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY;

public class JsonObjectToCredentialStatusTransformer extends AbstractJsonLdTransformer<JsonObject, CredentialStatus> {
    public JsonObjectToCredentialStatusTransformer() {
        super(JsonObject.class, CredentialStatus.class);
    }

    @Override
    public @Nullable CredentialStatus transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {


        var props = new HashMap<String, Object>();
        AtomicReference<String> id = new AtomicReference<>();
        AtomicReference<String> type = new AtomicReference<>();
        visitProperties(jsonObject, (s, jsonValue) -> {

            switch (s) {
                case CREDENTIAL_STATUS_ID_PROPERTY:
                    id.set(transformString(jsonValue, context));
                    break;
                case CREDENTIAL_STATUS_TYPE_PROPERTY:
                    type.set(transformString(jsonValue, context));
                    break;
                default:
                    props.put(s, transformGenericProperty(jsonValue, context));
                    break;
            }
        });

        return new CredentialStatus(id.get(), type.get(), props);
    }
}
