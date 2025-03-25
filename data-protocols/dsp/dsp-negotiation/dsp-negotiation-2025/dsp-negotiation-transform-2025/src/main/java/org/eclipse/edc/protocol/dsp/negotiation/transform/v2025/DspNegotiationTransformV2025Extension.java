/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.v2025;

import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationErrorTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationAckTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractAgreementMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractAgreementVerificationMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractNegotiationEventMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractNegotiationTerminationMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractNegotiationV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractOfferMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from.JsonObjectFromContractRequestMessageV2024Transformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2025_1;

/**
 * Provides the transformers for negotiation message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspNegotiationTransformV2025Extension.NAME)
public class DspNegotiationTransformV2025Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol 2025/1 Negotiation Transform Extension";

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

        var dspApiTransformerRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationErrorTransformer(builderFactory, DSP_NAMESPACE_V_2025_1));

        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementVerificationMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationEventMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractRequestMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationTerminationMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractOfferMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationAckTransformer(DSP_NAMESPACE_V_2025_1));

        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractRequestMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractOfferMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementVerificationMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationEventMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTerminationMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
    }
}