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
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferTerminationMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Map;

@Extension(value = JsonLdExtension.NAME)
@Provides({JsonLdTransformerRegistry.class})
public class DspTransferProcessTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer Process Transform Extension";

    @Inject
    private JsonLdTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        //from
        registry.register(new JsonObjectFromTransferProcessTransformer(builderFactory));
        registry.register(new JsonObjectFromTransferStartMessageTransformer(builderFactory));
        registry.register(new JsonObjectFromTransferCompletionMessageTransformer(builderFactory));
        registry.register(new JsonObjectFromTransferTerminationMessageTransformer(builderFactory));
        registry.register(new JsonObjectFromTransferRequestMessageTransformer(builderFactory));

        //to
        registry.register(new JsonObjectToTransferRequestMessageTransformer());
        registry.register(new JsonObjectToTransferCompletionMessageTransformer());
        registry.register(new JsonObjectToTransferStartMessageTransformer());
        registry.register(new JsonObjectToTransferTerminationMessageTransformer());
    }
}
