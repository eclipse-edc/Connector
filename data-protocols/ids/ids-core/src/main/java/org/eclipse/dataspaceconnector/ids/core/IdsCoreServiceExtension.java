/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceSettings;
import org.eclipse.dataspaceconnector.ids.core.transform.IdsTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.ids.core.version.ConnectorVersionProviderImpl;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the IDS Controller REST API.
 */
@Provides({ ConnectorVersionProvider.class, CatalogService.class, ConnectorService.class, IdsDescriptorService.class,
        CatalogService.class, ConnectorService.class, IdsTransformerRegistry.class })
public class IdsCoreServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_CATALOG_ID = "edc.ids.catalog.id";

    public static final String DEFAULT_EDC_IDS_CATALOG_ID = "urn:catalog:default";

    private static final String WARNING_USING_DEFAULT_SETTING = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String ERROR_INVALID_SETTING = "IDS Settings: Invalid setting for '%s'. Was %s'.";

    private Monitor monitor;
    @Inject
    private ContractOfferService contractOfferService;
    @Inject
    private IdentityService identityService;
    @Inject
    private OkHttpClient okHttpClient;

    @Override
    public String name() {
        return "IDS Core";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        List<String> settingErrors = new ArrayList<>();
        ConnectorServiceSettings connectorServiceSettings = null;
        String dataCatalogId = null;

        try {
            connectorServiceSettings = new ConnectorServiceSettings(serviceExtensionContext, monitor);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        try {
            dataCatalogId = resolveCatalogId(serviceExtensionContext);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        if (!settingErrors.isEmpty()) {
            throw new EdcException(String.join(", ", settingErrors));
        }

        IdsTransformerRegistry transformerRegistry = createTransformerRegistry();
        serviceExtensionContext.registerService(IdsTransformerRegistry.class, transformerRegistry);

        ConnectorVersionProvider connectorVersionProvider = createConnectorVersionProvider();
        serviceExtensionContext.registerService(ConnectorVersionProvider.class, connectorVersionProvider);

        CatalogService dataCatalogService = createDataCatalogService(dataCatalogId, contractOfferService);

        serviceExtensionContext.registerService(CatalogService.class, dataCatalogService);

        ConnectorService connectorService = createConnectorService(connectorServiceSettings, connectorVersionProvider, dataCatalogService);
        serviceExtensionContext.registerService(ConnectorService.class, connectorService);

        registerOther(serviceExtensionContext);
    }

    private void registerOther(ServiceExtensionContext context) {
        var descriptorService = new IdsDescriptorServiceImpl();
        context.registerService(IdsDescriptorService.class, descriptorService);
    }

    private IdsTransformerRegistry createTransformerRegistry() {
        return new IdsTransformerRegistryImpl();
    }

    private CatalogService createDataCatalogService(
            String dataCatalogId,
            ContractOfferService contractOfferService) {
        return new CatalogServiceImpl(
                monitor,
                dataCatalogId,
                contractOfferService
        );
    }

    private ConnectorService createConnectorService(
            ConnectorServiceSettings connectorServiceSettings,
            ConnectorVersionProvider connectorVersionProvider,
            CatalogService dataCatalogService) {

        return new ConnectorServiceImpl(
                monitor,
                connectorServiceSettings,
                connectorVersionProvider,
                dataCatalogService
        );
    }

    private ConnectorVersionProvider createConnectorVersionProvider() {
        return new ConnectorVersionProviderImpl();
    }

    private String resolveCatalogId(ServiceExtensionContext serviceExtensionContext) {
        String value = serviceExtensionContext.getSetting(EDC_IDS_CATALOG_ID, null);

        if (value == null) {
            monitor.warning(String.format(WARNING_USING_DEFAULT_SETTING, EDC_IDS_CATALOG_ID, DEFAULT_EDC_IDS_CATALOG_ID));
            value = DEFAULT_EDC_IDS_CATALOG_ID;
        }

        try {
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(value);
            if (idsId.getType() == IdsType.CATALOG) {
                return idsId.getValue();
            } else {
                throw new EdcException(String.format(ERROR_INVALID_SETTING, EDC_IDS_CATALOG_ID, value));
            }
        } catch (IllegalArgumentException e) {
            throw new EdcException(String.format(ERROR_INVALID_SETTING, EDC_IDS_CATALOG_ID, value));
        }
    }
}
