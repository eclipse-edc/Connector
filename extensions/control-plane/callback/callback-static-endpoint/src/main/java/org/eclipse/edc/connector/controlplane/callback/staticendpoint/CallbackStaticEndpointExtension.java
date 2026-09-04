/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.staticendpoint;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extension for configuring the static endpoints for callbacks
 */
@Extension(CallbackStaticEndpointExtension.NAME)
public class CallbackStaticEndpointExtension implements ServiceExtension {

    public static final String EDC_CALLBACK_SETTING_PREFIX = "edc.callback";

    static final String NAME = "Static callbacks extension";

    @Configuration(context = EDC_CALLBACK_SETTING_PREFIX)
    private Map<String, CallbackStaticConfiguration> configurationMap;
    @Inject
    private CallbackRegistry callbackRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        configurationMap.values().stream()
                .map(this::createCallbackAddress)
                .forEach(callbackRegistry::register);
    }

    private CallbackAddress createCallbackAddress(CallbackStaticConfiguration configuration) {
        if (configuration.authKey() != null && configuration.authCodeId() == null) {
            throw new EdcException("Static callback configuration: auth.codeid cannot be null if auth.key is provided");
        }

        var events = Arrays.stream(configuration.events().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return CallbackAddress.Builder.newInstance()
                .uri(configuration.uri())
                .transactional(configuration.transactional())
                .authKey(configuration.authKey())
                .authCodeId(configuration.authCodeId())
                .events(events)
                .build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Settings
    record CallbackStaticConfiguration(
            @Setting(
                    key = "uri",
                    description = "URI of the callback endpoint.")
            String uri,
            @Setting(
                    key = "events",
                    description = "Comma separated list of events to trigger the callback. If not provided, the callback will be triggered for all events.",
                    defaultValue = "")
            String events,
            @Setting(
                    key = "transactional",
                    description = "Whether the callback should be invoked in a transactional context. Default is false.",
                    defaultValue = "false"
            )
            boolean transactional,
            @Setting(
                    key = "auth.key",
                    description = "Authentication key to use for the callback. If not provided, no authentication will be used.",
                    required = false)
            String authKey,
            @Setting(
                    key = "auth.codeid",
                    description = "Vault Alias of the authentication token to use for the callback. If not provided, no authentication will be used.",
                    required = false)
            String authCodeId
    ) {
    }

}
