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

package org.eclipse.edc.protocol.dsp.transform;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.transform.type.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

@Extension(value = JsonLdExtension.NAME)
@Provides({JsonLdTransformerRegistry.class})
public class DspTransformExtension implements ServiceExtension {

    public static final String NAME = "DSP: Transform Extension";

    @Inject
    private TypeManager typeManager;

    @Inject
    private JsonLdTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var mapper = typeManager.getMapper("json-ld");

        registry.register(new JsonObjectFromContractNegotiationTransformer(builderFactory, mapper));
    }
}
