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

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.EventAssetListener;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.AssetDtoToAssetTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.AssetToAssetDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.DataAddressDtoToDataAddressTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservableImpl;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.Clock;

@Provides(AssetService.class)
public class AssetApiExtension implements ServiceExtension {

    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    AssetIndex assetIndex;

    @Inject
    AssetLoader assetLoader;

    @Inject
    ContractNegotiationStore contractNegotiationStore;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject
    TransactionContext transactionContext;

    @Inject
    EventRouter eventRouter;

    @Inject
    Clock clock;

    @Override
    public String name() {
        return "Data Management API: Asset";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var assetObservable = new AssetObservableImpl();
        assetObservable.registerListener(new EventAssetListener(clock, eventRouter));

        var assetService = new AssetServiceImpl(assetIndex, assetLoader, contractNegotiationStore, transactionContext, assetObservable);
        context.registerService(AssetService.class, assetService);

        transformerRegistry.register(new AssetDtoToAssetTransformer());
        transformerRegistry.register(new DataAddressDtoToDataAddressTransformer());
        transformerRegistry.register(new AssetToAssetDtoTransformer());

        webService.registerResource(config.getContextAlias(), new AssetApiController(monitor, assetService, transformerRegistry));
    }

}
