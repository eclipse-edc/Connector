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

package org.eclipse.edc.protocol.ids;

import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.service.CatalogServiceImpl;
import org.eclipse.edc.protocol.ids.service.ConnectorServiceImpl;
import org.eclipse.edc.protocol.ids.service.ConnectorServiceSettings;
import org.eclipse.edc.protocol.ids.service.DynamicAttributeTokenServiceImpl;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.service.ConnectorService;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the IDS Controller REST API.
 */
@Provides({ CatalogService.class, ConnectorService.class, DynamicAttributeTokenService.class })
@Extension(value = IdsCoreServiceExtension.NAME)
public class IdsCoreServiceExtension implements ServiceExtension {

    @Setting
    public static final String EDC_IDS_CATALOG_ID = "edc.ids.catalog.id";

    public static final String DEFAULT_EDC_IDS_CATALOG_ID = "urn:catalog:default";
    public static final String NAME = "IDS Core";
    private static final String WARNING_USING_DEFAULT_SETTING = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
    private static final String ERROR_INVALID_SETTING = "IDS Settings: Invalid setting for '%s'. Was %s'.";
    private Monitor monitor;

    @Inject
    private ContractOfferResolver contractOfferResolver;

    @Inject
    private IdentityService identityService;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        IdsTypeManagerUtil.customizeTypeManager(typeManager);

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

        var dataCatalogService = new CatalogServiceImpl(dataCatalogId, contractOfferResolver);
        context.registerService(CatalogService.class, dataCatalogService);

        var connectorService = new ConnectorServiceImpl(connectorServiceSettings, dataCatalogService);
        context.registerService(ConnectorService.class, connectorService);

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
