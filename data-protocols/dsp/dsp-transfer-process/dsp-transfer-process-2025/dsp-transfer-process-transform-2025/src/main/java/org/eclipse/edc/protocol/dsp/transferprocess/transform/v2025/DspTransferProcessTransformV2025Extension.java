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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.v2025;

import jakarta.json.Json;
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

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides the transformers for transferprocess message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransferProcessTransformV2025Extension.NAME)
public class DspTransferProcessTransformV2025Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol 2025/1 Transfer Process Transform Extension";

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
        registerTransformers();
    }

    private void registerTransformers() {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);

        dspRegistry.register(new JsonObjectFromTransferErrorTransformer(builderFactory, DSP_NAMESPACE_V_2025_1));

        dspRegistry.register(new JsonObjectToTransferRequestMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectToTransferCompletionMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectToTransferStartMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectToTransferTerminationMessageTransformer(DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectToTransferProcessAckTransformer(DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectToTransferSuspensionMessageTransformer(typeManager, JSON_LD, DSP_NAMESPACE_V_2025_1));

        dspRegistry.register(new JsonObjectFromTransferProcessV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectFromTransferRequestMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectFromTransferStartMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectFromTransferCompletionMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectFromTransferTerminationMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
        dspRegistry.register(new JsonObjectFromTransferSuspensionMessageV2024Transformer(builderFactory, DSP_NAMESPACE_V_2025_1));
    }

}