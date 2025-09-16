/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - name refactoring
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.asset;

import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.api.validation.DataAddressValidator;
import org.eclipse.edc.connector.controlplane.api.management.asset.v3.AssetApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.asset.v4.AssetApiV4Controller;
import org.eclipse.edc.connector.controlplane.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE;

@Extension(value = AssetApiExtension.NAME)
public class AssetApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Asset";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private AssetService assetService;

    @Inject
    private JsonObjectValidatorRegistry validator;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        validator.register(EDC_ASSET_TYPE, AssetValidator.instance());
        validator.register(EDC_DATA_ADDRESS_TYPE, DataAddressValidator.instance());

        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        webService.registerResource(ApiContext.MANAGEMENT, new AssetApiV3Controller(assetService,
                managementTypeTransformerRegistry, monitor, validator, participantContextSupplier));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AssetApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new AssetApiV4Controller(assetService,
                managementTypeTransformerRegistry, monitor, validator, participantContextSupplier));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AssetApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validator, ManagementApiJsonSchema.V4.version()));

    }
}
