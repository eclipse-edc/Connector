/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.web.spi.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebServiceSettingsTest {

    @Test
    void shouldDefineDefaultPath_whenNotSet() {
        var settings = WebServiceSettings.Builder.newInstance()
                .defaultPort(9999)
                .apiConfigKey("web.http.something")
                .contextAlias("alias")
                .build();

        assertThat(settings.getDefaultPath()).isEqualTo("/api/alias");
    }
}
