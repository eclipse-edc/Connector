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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;

public class JsonObjectToTransferRequestMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, TransferRequestMessage> {

    public JsonObjectToTransferRequestMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, TransferRequestMessage.class, namespace);
    }

    @Override
    public @Nullable TransferRequestMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var builder = TransferRequestMessage.Builder.newInstance();

        if (!transformMandatoryString(messageObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }

        transformString(messageObject.get(forNamespace(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM)), builder::contractId, context);
        transformString(messageObject.get(forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM)), builder::callbackAddress, context);

        var dataAddressObject = messageObject.get(forNamespace(DSPACE_PROPERTY_DATA_ADDRESS_TERM));
        if (dataAddressObject != null) {
            builder.dataDestination(transformObject(dataAddressObject, DataAddress.class, context));
        }

        transformString(messageObject.get(DCT_FORMAT_ATTRIBUTE), builder::transferType, context);

        return builder.build();
    }

}
