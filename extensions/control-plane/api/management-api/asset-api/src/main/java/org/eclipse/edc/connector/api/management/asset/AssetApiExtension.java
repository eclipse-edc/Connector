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

package org.eclipse.edc.connector.api.management.asset;

import org.eclipse.edc.api.validation.DataAddressDtoValidator;
import org.eclipse.edc.connector.api.management.asset.transform.AssetRequestDtoToAssetTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.AssetToAssetResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.AssetUpdateRequestWrapperDtoToAssetTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.JsonObjectToAssetEntryNewDtoTransformer;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.api.management.asset.validation.AssetEntryDtoValidator;
import org.eclipse.edc.connector.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_TYPE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_TYPE;

@Extension(value = AssetApiExtension.NAME)
public class AssetApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Asset";

    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration config;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private AssetService assetService;

    @Inject
    private DataAddressResolver dataAddressResolver;

    @Inject
    private JsonObjectValidatorRegistry validator;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        transformerRegistry.register(new AssetRequestDtoToAssetTransformer());
        transformerRegistry.register(new AssetUpdateRequestWrapperDtoToAssetTransformer());
        transformerRegistry.register(new AssetToAssetResponseDtoTransformer());
        transformerRegistry.register(new JsonObjectToAssetEntryNewDtoTransformer());

        validator.register(EDC_ASSET_ENTRY_DTO_TYPE, AssetEntryDtoValidator.assetEntryValidator());
        validator.register(EDC_ASSET_TYPE, AssetValidator.instance());
        validator.register(EDC_DATA_ADDRESS_TYPE, DataAddressDtoValidator.instance());

        webService.registerResource(config.getContextAlias(), new org.eclipse.edc.connector.api.management.asset.v2.AssetApiController(assetService, dataAddressResolver, transformerRegistry, monitor, validator));
        webService.registerResource(config.getContextAlias(), new AssetApiController(assetService, transformerRegistry, monitor, validator));
    }
}
