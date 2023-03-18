/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transform.type;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

/**
 * Creates a dspace:ContractAgreementMessage as {@link JsonObject} from {@link ContractAgreementRequest}.
 */
public class JsonObjectFromContractAgreementRequestTransformer extends AbstractJsonLdTransformer<ContractAgreementRequest, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementRequestTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractAgreementRequest.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable ContractAgreementRequest message, @NotNull TransformerContext context) {
        if (message == null) {
            return null;
        }

        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractAgreementMessage");

        builder.add(DSPACE_SCHEMA + "processId", message.getCorrelationId());
        builder.add(DSPACE_SCHEMA + "agreement", transformContractAgreement(message.getContractAgreement(), context));

        return builder.build();
    }

    private @Nullable JsonObject transformContractAgreement(ContractAgreement agreement, TransformerContext context) {
        return context.transform(agreement.getPolicy(), JsonObject.class);
    }

}
