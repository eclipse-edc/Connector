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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.serialization.IdsTypeManagerUtil;
import org.eclipse.dataspaceconnector.ids.core.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceSettings;
import org.eclipse.dataspaceconnector.ids.core.service.DynamicAttributeTokenServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.transform.IdsTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the IDS Controller REST API.
 */
@Provides({ CatalogService.class, ConnectorService.class, IdsDescriptorService.class,
        CatalogService.class, ConnectorService.class, IdsTransformerRegistry.class,
        DynamicAttributeTokenService.class})
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
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        IdsTypeManagerUtil.customizeTypeManager(context.getTypeManager());

        List<String> settingErrors = new ArrayList<>();
        ConnectorServiceSettings connectorServiceSettings = null;
        String dataCatalogId = null;

        try {
            connectorServiceSettings = new ConnectorServiceSettings(context, monitor);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        try {
            dataCatalogId = resolveCatalogId(context);
        } catch (EdcException e) {
            settingErrors.add(e.getMessage());
        }

        if (!settingErrors.isEmpty()) {
            throw new EdcException(String.join(", ", settingErrors));
        }

        context.registerService(IdsTransformerRegistry.class, new IdsTransformerRegistryImpl());

        var dataCatalogService = new CatalogServiceImpl(monitor, dataCatalogId, contractOfferService);
        context.registerService(CatalogService.class, dataCatalogService);

        var connectorService = new ConnectorServiceImpl(monitor, connectorServiceSettings, dataCatalogService);
        context.registerService(ConnectorService.class, connectorService);

        context.registerService(IdsDescriptorService.class, new IdsDescriptorServiceImpl());
    
        context.registerService(DynamicAttributeTokenService.class, new DynamicAttributeTokenServiceImpl(identityService));
    }

    private String resolveCatalogId(ServiceExtensionContext context) {
        var value = context.getSetting(EDC_IDS_CATALOG_ID, null);
        if (value == null) {
            monitor.warning(String.format(WARNING_USING_DEFAULT_SETTING, EDC_IDS_CATALOG_ID, DEFAULT_EDC_IDS_CATALOG_ID));
            value = DEFAULT_EDC_IDS_CATALOG_ID;
        }

        var result = IdsId.from(value);
        if (result.succeeded()) {
            var idsId = result.getContent();
            if (idsId.getType() == IdsType.CATALOG) {
                return idsId.getValue();
            }
        }

        throw new EdcException(String.format(ERROR_INVALID_SETTING, EDC_IDS_CATALOG_ID, value));
    }
}
