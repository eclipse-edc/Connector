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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer;

import jakarta.json.Json;
import org.eclipse.edc.core.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferProcessAckTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferTerminationMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

/**
 * Provides the transformers for transferprocess message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransferProcessTransformExtension.NAME)
public class DspTransferProcessTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer Process Transform Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspApiTransformerRegistry = registry.forContext("dsp-api");
        dspApiTransformerRegistry.register(new JsonObjectFromTransferProcessTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromTransferStartMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromTransferCompletionMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromTransferTerminationMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromTransferRequestMessageTransformer(builderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(builderFactory));

        dspApiTransformerRegistry.register(new JsonObjectToTransferRequestMessageTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToTransferCompletionMessageTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToTransferStartMessageTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToTransferTerminationMessageTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToTransferProcessAckTransformer());
    }
}
