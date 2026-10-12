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

package org.eclipse.edc.connector.controlplane.transform.edc.contractagreement.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement.RETIRE_AGREEMENT_REASON;

public class JsonObjectToRetireAgreementTransformer extends JsonLdToModelTransformer<JsonObject, RetireAgreement> {

    public JsonObjectToRetireAgreementTransformer() {
        super(JsonObject.class, RetireAgreement.class);
    }

    @Override
    public @Nullable RetireAgreement transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var reason = transformString(input.get(RETIRE_AGREEMENT_REASON), context);

        return new RetireAgreement(reason);
    }

}
