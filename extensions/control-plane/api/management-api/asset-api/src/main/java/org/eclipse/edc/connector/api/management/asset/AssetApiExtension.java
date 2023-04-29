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

import org.eclipse.edc.connector.api.management.asset.transform.AssetRequestDtoToAssetTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.AssetToAssetResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.AssetUpdateRequestWrapperDtoToAssetTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.DataAddressDtoToDataAddressTransformer;
import org.eclipse.edc.connector.api.management.asset.transform.DataAddressToDataAddressDtoTransformer;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

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
    private JsonLd jsonLdService;

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
        transformerRegistry.register(new DataAddressDtoToDataAddressTransformer());
        transformerRegistry.register(new DataAddressToDataAddressDtoTransformer());

        webService.registerResource(config.getContextAlias(), new AssetApiController(monitor, assetService, dataAddressResolver, transformerRegistry));
        webService.registerResource(config.getContextAlias(), new AssetNewApiController(assetService, dataAddressResolver, transformerRegistry, jsonLdService));
    }

}
