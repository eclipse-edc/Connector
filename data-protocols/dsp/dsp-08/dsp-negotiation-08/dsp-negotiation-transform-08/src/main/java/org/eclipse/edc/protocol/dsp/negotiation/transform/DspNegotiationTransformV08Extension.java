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
 *       Cofinity-X - refactor DSP module structure to make versions pluggable
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform;

import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationErrorTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationAckTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractRequestMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;

/**
 * Provides the transformers for DSP v0.8 negotiation message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspNegotiationTransformV08Extension.NAME)
public class DspNegotiationTransformV08Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation Transform v08 Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers();
    }

    private void registerTransformers() {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspApiTransformerRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_08);
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationErrorTransformer(builderFactory, DSP_NAMESPACE_V_08));

        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementVerificationMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationEventMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractRequestMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationTerminationMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractOfferMessageTransformer(DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationAckTransformer(DSP_NAMESPACE_V_08));
        
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractRequestMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractOfferMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementVerificationMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationEventMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTerminationMessageTransformer(builderFactory));
    }
}