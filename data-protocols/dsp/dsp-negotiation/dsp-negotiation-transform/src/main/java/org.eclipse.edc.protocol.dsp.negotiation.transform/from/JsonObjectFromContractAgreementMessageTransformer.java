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

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;


/**
 * Creates a dspace:ContractAgreementMessage as {@link JsonObject} from {@link ContractAgreementMessage}.
 */
public class JsonObjectFromContractAgreementMessageTransformer extends AbstractJsonLdTransformer<ContractAgreementMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractAgreementMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreementMessage object, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE);

        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, object.getProcessId());

        var policy = context.transform(object.getContractAgreement().getPolicy(), JsonObject.class);
        if (policy == null) {
            context.reportProblem("Cannot transform from ContractAgreementMessage with null policy");
            return null;
        }

        builder.add(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT, policy);

        return builder.build();
    }

}
