/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.client.EmbeddedDataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.client.RemoteDataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.util.string.StringUtils;

import java.util.Objects;

import static java.lang.String.format;

@Extension(value = "DataPlane Selector client")
public class DataPlaneSelectorClientExtension implements ServiceExtension {

    @Setting(value = "The DataPlane selector api URL")
    static final String DPF_SELECTOR_URL_SETTING = "edc.dpf.selector.url";

    private static final String DEFAULT_DATAPLANE_SELECTOR_STRATEGY = "random";
    @Setting(value = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime", defaultValue = DEFAULT_DATAPLANE_SELECTOR_STRATEGY)
    private static final String DPF_SELECTOR_STRATEGY = "edc.dataplane.client.selector.strategy";

    @Inject(required = false)
    private DataPlaneSelectorService selector;

    @Inject(required = false)
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;
    
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Provider
    public DataPlaneSelectorClient dataPlaneSelectorClient(ServiceExtensionContext context) {
        var url = context.getConfig().getString(DPF_SELECTOR_URL_SETTING, null);
        var monitor = context.getMonitor();
        var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, DEFAULT_DATAPLANE_SELECTOR_STRATEGY);

        DataPlaneSelectorClient client;
        if (StringUtils.isNullOrBlank(url)) {
            Objects.requireNonNull(selector, format("If [%s] is not specified, a DataPlaneSelectorService instance must be provided", DPF_SELECTOR_URL_SETTING));
            client = new EmbeddedDataPlaneSelectorClient(selector, selectionStrategy);
            monitor.debug("Using embedded DPF selector");
        } else {
            Objects.requireNonNull(httpClient, format("If [%s] is specified, an EdcHttpClient instance must be provided", DPF_SELECTOR_URL_SETTING));
            client = new RemoteDataPlaneSelectorClient(httpClient, url, typeManager.getMapper(), typeTransformerRegistry, selectionStrategy);
            monitor.debug("Using remote DPF selector");
        }

        return client;
    }
}
