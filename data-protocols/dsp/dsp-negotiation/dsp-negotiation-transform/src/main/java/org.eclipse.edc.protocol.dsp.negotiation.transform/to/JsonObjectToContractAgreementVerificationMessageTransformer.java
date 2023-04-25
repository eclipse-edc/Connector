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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractAgreementVerificationMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementVerificationMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractAgreementVerificationMessage> {

    public JsonObjectToContractAgreementVerificationMessageTransformer() {
        super(JsonObject.class, ContractAgreementVerificationMessage.class);
    }

    @Override
    public @Nullable ContractAgreementVerificationMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractAgreementVerificationMessage.Builder.newInstance();
        builder.protocol(DATASPACE_PROTOCOL_HTTP);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context);
        // TODO add mapping of cred:credentialSubject and sec:proof with signature processes

        return builder.build();
    }

}
