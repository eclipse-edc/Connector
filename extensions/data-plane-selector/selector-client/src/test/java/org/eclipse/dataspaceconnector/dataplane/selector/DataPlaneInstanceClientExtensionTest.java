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
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.client.EmbeddedDataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.client.RemoteDataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.junit.extensions.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneInstanceClientExtensionTest {

    private static final String EDC_DPF_SELECTOR_URL_SETTING = "edc.dpf.selector.url";
    private DataPlaneInstanceClientExtension extension;
    private ServiceExtensionContext context;
    private ObjectFactory factory;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {

        this.context = context;
        this.factory = factory;
    }

    @Test
    void initialize_noSetting_shouldUseEmbedded() {
        context.registerService(DataPlaneSelectorService.class, mock(DataPlaneSelectorService.class));
        extension = factory.constructInstance(DataPlaneInstanceClientExtension.class);

        extension.initialize(context);

        var client = context.getService(DataPlaneSelectorClient.class);
        assertThat(client).isInstanceOf(EmbeddedDataPlaneSelectorClient.class);
    }

    @Test
    void initialize_noSetting_shouldUseEmbedded_serviceMissing() {
        // DataPlaneSelectorService not registered, should raise exception
        extension = factory.constructInstance(DataPlaneInstanceClientExtension.class);

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(NullPointerException.class)
                .hasMessage("If [" + EDC_DPF_SELECTOR_URL_SETTING + "] is not specified, a DataPlaneSelectorService instance must be provided");

        var client = context.getService(DataPlaneSelectorClient.class, true);
        assertThat(client).isNull();
    }

    @Test
    void initialize_withSetting() {
        var config = ConfigFactory.fromMap(Map.of(EDC_DPF_SELECTOR_URL_SETTING, "http://someurl.com:1234"));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);

        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        context.registerService(RetryPolicy.class, mock(RetryPolicy.class));
        extension = factory.constructInstance(DataPlaneInstanceClientExtension.class);

        extension.initialize(context);

        var client = context.getService(DataPlaneSelectorClient.class);
        assertThat(client).isInstanceOf(RemoteDataPlaneSelectorClient.class);
    }

    @Test
    void initialize_withSetting_okHttpMissing() {
        var config = ConfigFactory.fromMap(Map.of(EDC_DPF_SELECTOR_URL_SETTING, "http://someurl.com:1234"));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);

        context.registerService(RetryPolicy.class, mock(RetryPolicy.class));
        extension = factory.constructInstance(DataPlaneInstanceClientExtension.class);

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(NullPointerException.class)
                .hasMessage("If [" + EDC_DPF_SELECTOR_URL_SETTING + "] is specified, a OkHttpClient instance must be provided");

        var client = context.getService(DataPlaneSelectorClient.class, true);
        assertThat(client).isNull();
    }

    @Test
    void initialize_withSetting_retryPolicyMissing() {

        var config = ConfigFactory.fromMap(Map.of(EDC_DPF_SELECTOR_URL_SETTING, "http://someurl.com:1234"));
        context = spy(context);
        when(context.getConfig()).thenReturn(config);

        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        extension = factory.constructInstance(DataPlaneInstanceClientExtension.class);

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(NullPointerException.class)
                .hasMessage("If [" + EDC_DPF_SELECTOR_URL_SETTING + "] is specified, a RetryPolicy instance must be provided");

        var client = context.getService(DataPlaneSelectorClient.class, true);
        assertThat(client).isNull();
    }
}