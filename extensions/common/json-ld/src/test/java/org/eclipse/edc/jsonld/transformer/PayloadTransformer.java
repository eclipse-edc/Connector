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

package org.eclipse.edc.jsonld.transformer;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.Namespaces;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PayloadTransformer extends AbstractJsonLdTransformer<JsonObject, Payload> {
    public PayloadTransformer() {
        super(JsonObject.class, Payload.class);
    }

    @Override
    public @Nullable Payload transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var payload = new Payload();
        var nameObj = jsonObject.get(Namespaces.EDC_SCHEMA + "name");
        var ageObj = jsonObject.get(Namespaces.EDC_SCHEMA + "age");
        transformString(nameObj, payload::setName, context);
        var age = (Double) transformGenericProperty(ageObj, context);
        payload.setAge(age.intValue());
        return payload;
    }
}
