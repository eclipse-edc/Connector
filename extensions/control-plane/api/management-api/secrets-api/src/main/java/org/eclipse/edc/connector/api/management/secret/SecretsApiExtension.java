/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.api.management.secret.transform.JsonObjectFromSecretTransformer;
import org.eclipse.edc.connector.api.management.secret.transform.JsonObjectToSecretTransformer;
import org.eclipse.edc.connector.api.management.secret.v3.SecretsApiV3Controller;
import org.eclipse.edc.connector.api.management.secret.v4.SecretsApiV4Controller;
import org.eclipse.edc.connector.api.management.secret.validation.SecretsValidator;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;

@Extension(value = SecretsApiExtension.NAME)
public class SecretsApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Secret";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private SecretService secretService;

    @Inject
    private JsonObjectValidatorRegistry validator;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validator.register(EDC_SECRET_TYPE, SecretsValidator.instance());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        managementApiTransformerRegistry.register(new JsonObjectFromSecretTransformer(jsonBuilderFactory));
        managementApiTransformerRegistry.register(new JsonObjectToSecretTransformer());

        webService.registerResource(ApiContext.MANAGEMENT, new SecretsApiV3Controller(secretService, managementApiTransformerRegistry, validator));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, SecretsApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new SecretsApiV4Controller(secretService, managementApiTransformerRegistry, validator));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, SecretsApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validator, ManagementApiJsonSchema.V4.version()));

    }

}
