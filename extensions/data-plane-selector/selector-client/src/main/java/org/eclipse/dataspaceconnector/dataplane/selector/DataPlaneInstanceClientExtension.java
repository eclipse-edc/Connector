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

package org.eclipse.dataspaceconnector.dataplane.selector;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.client.EmbeddedDataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.client.RemoteDataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;

@Provides(DataPlaneSelectorClient.class)
public class DataPlaneInstanceClientExtension implements ServiceExtension {

    @EdcSetting
    private static final String DPF_SELECTOR_URL_SETTING = "edc.dpf.selector.url";

    @Inject(required = false)
    private DataPlaneSelectorService selector;

    @Inject(required = false)
    private OkHttpClient okHttpClient;

    @Inject(required = false)
    private RetryPolicy retryPolicy;


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
            Objects.requireNonNull(okHttpClient, format("If [%s] is specified, a OkHttpClient instance must be provided", DPF_SELECTOR_URL_SETTING));
            Objects.requireNonNull(retryPolicy, format("If [%s] is specified, a RetryPolicy instance must be provided", DPF_SELECTOR_URL_SETTING));
            client = new RemoteDataPlaneSelectorClient(okHttpClient, url, retryPolicy, context.getTypeManager().getMapper());
            monitor.debug("Using remote DPF selector");
        }

        context.registerService(DataPlaneSelectorClient.class, client);
    }
}
