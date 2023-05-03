/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api;

import jakarta.json.Json;
import org.eclipse.edc.api.transformer.CallbackAddressDtoToCallbackAddressTransformer;
import org.eclipse.edc.api.transformer.CriterionDtoToCriterionTransformer;
import org.eclipse.edc.api.transformer.CriterionToCriterionDtoTransformer;
import org.eclipse.edc.api.transformer.JsonObjectFromCriterionDtoTransformer;
import org.eclipse.edc.api.transformer.JsonObjectToCriterionDtoTransformer;
import org.eclipse.edc.api.transformer.QuerySpecDtoToQuerySpecTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension(value = ApiCoreExtension.NAME)
public class ApiCoreExtension implements ServiceExtension {

    public static final String NAME = "API Core";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new QuerySpecDtoToQuerySpecTransformer());
        transformerRegistry.register(new CriterionToCriterionDtoTransformer());
        transformerRegistry.register(new CriterionDtoToCriterionTransformer());
        transformerRegistry.register(new CallbackAddressDtoToCallbackAddressTransformer());
        var mapper = typeManager.getMapper(JSON_LD);
        var jsonFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectFromCriterionDtoTransformer(jsonFactory, mapper));
        transformerRegistry.register(new JsonObjectToCriterionDtoTransformer());
    }
}
