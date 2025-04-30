/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferErrorTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferProcessAckTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferSuspensionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferCompletionMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferProcessV2024Transformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferRequestMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferStartMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferSuspensionMessageV2024Transformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferTerminationMessageV2024Transformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides the transformers for transferprocess message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransferProcessTransformV2024Extension.NAME)
public class DspTransferProcessTransformV2024Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer Process Transform v2024/1 Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerV2024transformers();

        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_2024_1, DSP_NAMESPACE_V_2024_1);
    }

    private void registerTransformers(String version, JsonLdNamespace namespace) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspRegistry = registry.forContext(version);

        dspRegistry.register(new JsonObjectFromTransferErrorTransformer(builderFactory, namespace));

        dspRegistry.register(new JsonObjectToTransferRequestMessageTransformer(namespace));
        dspRegistry.register(new JsonObjectToTransferCompletionMessageTransformer(namespace));
        dspRegistry.register(new JsonObjectToTransferStartMessageTransformer(namespace));
        dspRegistry.register(new JsonObjectToTransferTerminationMessageTransformer(namespace));
        dspRegistry.register(new JsonObjectToTransferProcessAckTransformer(namespace));
        dspRegistry.register(new JsonObjectToTransferSuspensionMessageTransformer(typeManager, JSON_LD, namespace));
    }

    private void registerV2024transformers() {
        var builderFactory = Json.createBuilderFactory(Map.of());
        var dspRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_2024_1);
        dspRegistry.register(new JsonObjectFromTransferProcessV2024Transformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferRequestMessageV2024Transformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferStartMessageV2024Transformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferCompletionMessageV2024Transformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferTerminationMessageV2024Transformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferSuspensionMessageV2024Transformer(builderFactory));

    }
}