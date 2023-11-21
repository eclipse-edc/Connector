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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorClientExtension.DPF_SELECTOR_URL_SETTING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorClientExtensionTest {

    private DataPlaneSelectorClientExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        this.context = spy(context);
    }

    @Test
    void initialize_noSetting_shouldUseEmbedded(ObjectFactory factory) {
        context.registerService(DataPlaneSelectorService.class, mock(DataPlaneSelectorService.class));
        extension = factory.constructInstance(DataPlaneSelectorClientExtension.class);

        var client = extension.dataPlaneSelectorClient(context);

        assertThat(client).isInstanceOf(EmbeddedDataPlaneSelectorClient.class);
    }

    @Test
    void initialize_noSetting_shouldUseEmbedded_serviceMissing(ObjectFactory factory) {
        context.registerService(DataPlaneSelectorService.class, null);
        extension = factory.constructInstance(DataPlaneSelectorClientExtension.class);

        assertThatThrownBy(() -> extension.dataPlaneSelectorClient(context)).isInstanceOf(NullPointerException.class)
                .hasMessage("If [" + DPF_SELECTOR_URL_SETTING + "] is not specified, a DataPlaneSelectorService instance must be provided");
    }

    @Test
    void initialize_withSetting(ObjectFactory factory) {
        var config = ConfigFactory.fromMap(Map.of(DPF_SELECTOR_URL_SETTING, "http://someurl.com:1234"));
        when(context.getConfig()).thenReturn(config);

        context.registerService(EdcHttpClient.class, mock(EdcHttpClient.class));
        extension = factory.constructInstance(DataPlaneSelectorClientExtension.class);

        var client = extension.dataPlaneSelectorClient(context);

        assertThat(client).isInstanceOf(RemoteDataPlaneSelectorClient.class);
    }

    @Test
    void initialize_withSetting_httpClientMissing(ObjectFactory factory) {
        context.registerService(EdcHttpClient.class, null);
        var config = ConfigFactory.fromMap(Map.of(DPF_SELECTOR_URL_SETTING, "http://someurl.com:1234"));
        when(context.getConfig()).thenReturn(config);

        extension = factory.constructInstance(DataPlaneSelectorClientExtension.class);

        assertThatThrownBy(() -> extension.dataPlaneSelectorClient(context)).isInstanceOf(NullPointerException.class)
                .hasMessage("If [" + DPF_SELECTOR_URL_SETTING + "] is specified, an EdcHttpClient instance must be provided");
    }

}
