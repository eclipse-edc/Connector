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

package org.eclipse.edc.protocol.dsp.transferprocess.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferCompletionMessageTransformer;
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
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides the transformers for transferprocess message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransferProcessTransformExtension.NAME)
public class DspTransferProcessTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer Process Transform Extension";

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
        var objectMapper = typeManager.getMapper(JSON_LD);

        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_08, objectMapper);
        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_2024_1, objectMapper);
    }

    private void registerTransformers(String version, ObjectMapper objectMapper) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspRegistry = registry.forContext(version);

        dspRegistry.register(new JsonObjectFromTransferProcessTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferStartMessageTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferCompletionMessageTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferTerminationMessageTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferRequestMessageTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromTransferSuspensionMessageTransformer(builderFactory));
        dspRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory));

        dspRegistry.register(new JsonObjectToTransferRequestMessageTransformer());
        dspRegistry.register(new JsonObjectToTransferCompletionMessageTransformer());
        dspRegistry.register(new JsonObjectToTransferStartMessageTransformer());
        dspRegistry.register(new JsonObjectToTransferTerminationMessageTransformer());
        dspRegistry.register(new JsonObjectToTransferProcessAckTransformer());
        dspRegistry.register(new JsonObjectToTransferSuspensionMessageTransformer(objectMapper));
    }

}