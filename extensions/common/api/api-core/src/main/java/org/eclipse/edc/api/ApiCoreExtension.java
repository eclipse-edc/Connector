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
import org.eclipse.edc.api.transformer.JsonObjectFromCallbackAddressTransformer;
import org.eclipse.edc.api.transformer.JsonObjectFromIdResponseTransformer;
import org.eclipse.edc.api.transformer.JsonObjectToCallbackAddressTransformer;
import org.eclipse.edc.api.validation.QuerySpecValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.Map;

import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;

@Extension(value = ApiCoreExtension.NAME)
public class ApiCoreExtension implements ServiceExtension {

    public static final String NAME = "API Core";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());

        transformerRegistry.register(new JsonObjectFromCallbackAddressTransformer(jsonFactory));
        transformerRegistry.register(new JsonObjectFromIdResponseTransformer(jsonFactory));
        transformerRegistry.register(new JsonObjectToCallbackAddressTransformer());

        validatorRegistry.register(EDC_QUERY_SPEC_TYPE, QuerySpecValidator.instance());
    }
}
