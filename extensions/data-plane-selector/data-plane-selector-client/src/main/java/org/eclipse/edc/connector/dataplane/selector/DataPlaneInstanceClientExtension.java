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
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.util.string.StringUtils;

import java.util.Objects;

import static java.lang.String.format;

@Provides(DataPlaneSelectorClient.class)
@Extension(value = "DataPlane instance client")
public class DataPlaneInstanceClientExtension implements ServiceExtension {

    @Setting(value = "The URL of the DataPlane instance selector endpoint, which is used to determine whether to use an embedded or remote DataPlaneSelectorClient")
    private static final String DPF_SELECTOR_URL_SETTING = "edc.dpf.selector.url";

    @Inject(required = false)
    private DataPlaneSelectorService selector;

    @Inject(required = false)
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var url = context.getConfig().getString(DPF_SELECTOR_URL_SETTING, null);
        var monitor = context.getMonitor();

        DataPlaneSelectorClient client;
        if (StringUtils.isNullOrEmpty(url)) {
            Objects.requireNonNull(selector, format("If [%s] is not specified, a DataPlaneSelectorService instance must be provided", DPF_SELECTOR_URL_SETTING));
            client = new EmbeddedDataPlaneSelectorClient(selector);
            monitor.debug("Using embedded DPF selector");
        } else {
            Objects.requireNonNull(httpClient, format("If [%s] is specified, an EdcHttpClient instance must be provided", DPF_SELECTOR_URL_SETTING));
            client = new RemoteDataPlaneSelectorClient(httpClient, url, typeManager.getMapper());
            monitor.debug("Using remote DPF selector");
        }

        context.registerService(DataPlaneSelectorClient.class, client);
    }
}
