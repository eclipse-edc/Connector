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
 *       Cofinity-X - make DSP versions pluggable
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform;

import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferErrorTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferSuspensionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferProcessAckTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferSuspensionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferTerminationMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides the transformers for DSP v0.8 transferprocess message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransferProcessTransformV08Extension.NAME)
public class DspTransferProcessTransformV08Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer Process Transform v08 Extension";

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

        var dspRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_08);

        dspRegistry.register(new JsonObjectFromTransferErrorTransformer(builderFactory, DSP_NAMESPACE_V_08));

        dspRegistry.register(new JsonObjectToTransferRequestMessageTransformer(DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectToTransferCompletionMessageTransformer(DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectToTransferStartMessageTransformer(DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectToTransferTerminationMessageTransformer(DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectToTransferProcessAckTransformer(DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectToTransferSuspensionMessageTransformer(typeManager, JSON_LD, DSP_NAMESPACE_V_08));

        dspRegistry.register(new JsonObjectFromTransferProcessTransformer(builderFactory, DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectFromTransferRequestMessageTransformer(builderFactory, DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectFromTransferStartMessageTransformer(builderFactory, DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectFromTransferCompletionMessageTransformer(builderFactory, DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectFromTransferTerminationMessageTransformer(builderFactory, DSP_NAMESPACE_V_08));
        dspRegistry.register(new JsonObjectFromTransferSuspensionMessageTransformer(builderFactory, DSP_NAMESPACE_V_08));
    }
}