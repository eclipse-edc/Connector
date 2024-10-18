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

package org.eclipse.edc.protocol.dsp.negotiation.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryIdNotBlank;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;

/**
 * Validator for {@link ContractOfferMessage} Json-LD representation
 */
public class ContractOfferMessageValidator {
    public static Validator<JsonObject> instance() {
        return instance(DSP_NAMESPACE_V_08);
    }

    public static Validator<JsonObject> instance(JsonLdNamespace namespace) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new TypeIs(path, namespace.toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM)))
                .verifyObject(namespace.toIri(DSPACE_PROPERTY_OFFER_TERM), v -> v
                        .verifyId(MandatoryIdNotBlank::new)
                        .verify(ODRL_TARGET_ATTRIBUTE, MandatoryObject::new)
                        .verifyObject(ODRL_TARGET_ATTRIBUTE, b -> b.verifyId(MandatoryIdNotBlank::new))
                )
                .verify(namespace.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), MandatoryValue::new)
                .build();
    }
}
