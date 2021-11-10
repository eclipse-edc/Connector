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
import org.eclipse.dataspaceconnector.ids.core.daps.DapsServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.message.DataRequestMessageSender;
import org.eclipse.dataspaceconnector.ids.core.message.IdsRemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.ids.core.message.QueryMessageSender;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsPolicyServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceSettings;
import org.eclipse.dataspaceconnector.ids.core.service.DataCatalogServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.transform.TransformerRegistryImpl;
import org.eclipse.dataspaceconnector.ids.core.version.ConnectorVersionProviderImpl;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implements the IDS Controller REST API.
 */
public class IdsCoreServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_CATALOG_ID = "edc.ids.catalog.id";

    public static final String DEFAULT_EDC_IDS_CATALOG_ID = "urn:catalog:default";

    private static final String WARNING_USING_DEFAULT_SETTING = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String ERROR_INVALID_SETTING = "IDS Settings: Invalid setting for '%s'. Was %s'.";

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("edc:ids:core");
    }

    @Override
    public Set<String> requires() {
        return Set.of(IdentityService.FEATURE, "edc:core:contract", "dataspaceconnector:http-client", "dataspaceconnector:transferprocessstore");
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

        ContractOfferService contractOfferService = serviceExtensionContext.getService(ContractOfferService.class);

        TransformerRegistry transformerRegistry = createTransformerRegistry();
        serviceExtensionContext.registerService(TransformerRegistry.class, transformerRegistry);

        ConnectorVersionProvider connectorVersionProvider = createConnectorVersionProvider();
        serviceExtensionContext.registerService(ConnectorVersionProvider.class, connectorVersionProvider);

        DataCatalogService dataCatalogService = createDataCatalogService(dataCatalogId, contractOfferService);

        serviceExtensionContext.registerService(DataCatalogService.class, dataCatalogService);

        ConnectorService connectorService = createConnectorService(connectorServiceSettings, connectorVersionProvider, dataCatalogService);
        serviceExtensionContext.registerService(ConnectorService.class, connectorService);

        registerOther(serviceExtensionContext);

        monitor.info("Initialized IDS Core extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Core extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Core extension");
    }

    private void registerOther(ServiceExtensionContext context) {
        var descriptorService = new IdsDescriptorServiceImpl();
        context.registerService(IdsDescriptorService.class, descriptorService);

        var identityService = context.getService(IdentityService.class);
        var connectorId = context.getConnectorId();
        var dapsService = new DapsServiceImpl(connectorId, identityService);
        context.registerService(DapsService.class, dapsService);

        var policyService = new IdsPolicyServiceImpl();
        context.registerService(IdsPolicyService.class, policyService);

        assembleIdsDispatcher(connectorId, context, identityService);
    }

    /**
     * Assembles the IDS remote message dispatcher and its senders.
     */
    private void assembleIdsDispatcher(String connectorId, ServiceExtensionContext context, IdentityService identityService) {
        var processStore = context.getService(TransferProcessStore.class);
        var vault = context.getService(Vault.class);
        var httpClient = context.getService(OkHttpClient.class);

        var mapper = context.getTypeManager().getMapper();

        var monitor = context.getMonitor();

        var dispatcher = new IdsRemoteMessageDispatcher();

        dispatcher.register(new QueryMessageSender(connectorId, identityService, httpClient, mapper, monitor));
        dispatcher.register(new DataRequestMessageSender(connectorId, identityService, processStore, vault, httpClient, mapper, monitor));

        var registry = context.getService(RemoteMessageDispatcherRegistry.class);
        registry.register(dispatcher);
    }

    private TransformerRegistry createTransformerRegistry() {
        return new TransformerRegistryImpl();
    }

    private DataCatalogService createDataCatalogService(
            String dataCatalogId,
            ContractOfferService contractOfferService) {
        return new DataCatalogServiceImpl(
                monitor,
                dataCatalogId,
                contractOfferService
        );
    }

    private ConnectorService createConnectorService(
            ConnectorServiceSettings connectorServiceSettings,
            ConnectorVersionProvider connectorVersionProvider,
            DataCatalogService dataCatalogService) {

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
