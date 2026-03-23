/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.net.URI;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Settings
record SignalingApiConfiguration(
        @Setting(
                key = "web.http." + ApiContext.SIGNALING + ".port",
                description = "Port for " + ApiContext.SIGNALING + " api context",
                defaultValue = DEFAULT_SIGNALING_PORT + "")
        int port,
        @Setting(
                key = "web.http." + ApiContext.SIGNALING + ".path",
                description = "Path for " + ApiContext.SIGNALING + " api context",
                defaultValue = DEFAULT_SIGNALING_PATH)
        String path,
        @Setting(
                key = "web.http." + ApiContext.SIGNALING + ".public.uri",
                description = "Public uri for " + ApiContext.SIGNALING + " api context. If not defined, the 'localhost' one will be used.",
                required = false)
        String publicUri
) {

    private static final String DEFAULT_SIGNALING_PATH = "/api/signaling";
    private static final int DEFAULT_SIGNALING_PORT = 8182;

    public URI createPublicUri() {
        var callbackAddress = ofNullable(publicUri).orElseGet(() -> format("http://localhost:%s%s", port(), path()));

        try {
            return URI.create(callbackAddress);
        } catch (IllegalArgumentException e) {
            throw new EdcException("Error creating signaling endpoint url", e);
        }
    }
}
