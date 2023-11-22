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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorClientExtension.DPF_SELECTOR_URL_SETTING;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorClientExtensionTest {

    @Test
    void dataPlaneSelectorService_shouldReturnRemoteService(DataPlaneSelectorClientExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(DPF_SELECTOR_URL_SETTING, "http://any")));

        var client = extension.dataPlaneSelectorService(context);

        assertThat(client).isInstanceOf(RemoteDataPlaneSelectorService.class);
    }

    @Test
    void dataPlaneSelectorService_shouldThrowException_whenUrlNotConfigured(DataPlaneSelectorClientExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(emptyMap()));

        assertThatThrownBy(() -> extension.dataPlaneSelectorService(context)).isInstanceOf(EdcException.class)
                .hasMessageContaining("No setting found");
    }

}
