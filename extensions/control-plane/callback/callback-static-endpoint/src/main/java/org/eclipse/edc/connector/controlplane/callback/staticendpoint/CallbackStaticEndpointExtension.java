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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Extension for configuring the static endpoints for callbacks
 */
@Extension(CallbackStaticEndpointExtension.NAME)
public class CallbackStaticEndpointExtension implements ServiceExtension {

    public static final String EDC_CALLBACK_SETTING_PREFIX = "edc.callback";
    public static final String EDC_CALLBACK_SETTING_ALIAS = EDC_CALLBACK_SETTING_PREFIX + ".<cbAlias>.";
    @Setting(context = EDC_CALLBACK_SETTING_ALIAS, description = "URI of the callback endpoint.")
    public static final String EDC_CALLBACK_URI = "uri";
    @Setting(context = EDC_CALLBACK_SETTING_ALIAS, description = "Comma separated list of events to trigger the callback. If not provided, the callback will be triggered for all events.")
    public static final String EDC_CALLBACK_EVENTS = "events";
    @Setting(context = EDC_CALLBACK_SETTING_ALIAS, description = "Whether the callback should be invoked in a transactional context. Default is false.")
    public static final String EDC_CALLBACK_TRANSACTIONAL = "transactional";
    @Deprecated(since = "0.17.0")
    public static final String EDC_CALLBACK_AUTH_KEY_LEGACY = "auth-key";
    @Deprecated(since = "0.17.0")
    public static final String EDC_CALLBACK_AUTH_CODE_ID_LEGACY = "auth-code-id";

    @Setting(context = EDC_CALLBACK_SETTING_ALIAS, description = "Authentication key to use for the callback. If not provided, no authentication will be used.")
    public static final String EDC_CALLBACK_AUTH_KEY = "auth.key";
    @Setting(context = EDC_CALLBACK_SETTING_ALIAS, description = "Vault Alias of the authentication token to use for the callback. If not provided, no authentication will be used.")
    public static final String EDC_CALLBACK_AUTH_CODE_ID = "auth.codeid";
    static final String NAME = "Static callbacks extension";
    @Inject
    private CallbackRegistry callbackRegistry;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.getConfig(EDC_CALLBACK_SETTING_PREFIX)
                .partition()
                .map(this::configureCallback)
                .forEach(callbackRegistry::register);

    }

    @Override
    public String name() {
        return NAME;
    }

    public CallbackAddress configureCallback(Config config) {
        var events = Arrays.stream(config.getString(EDC_CALLBACK_EVENTS).split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        var authKey = config.getString(EDC_CALLBACK_AUTH_KEY, null);

        if (authKey == null) {
            authKey = config.getString(EDC_CALLBACK_AUTH_KEY_LEGACY, null);

            if (authKey == null) {
                monitor.warning("Using deprecated configuration '%s'. Please switch to '%s'".formatted(EDC_CALLBACK_AUTH_KEY_LEGACY, EDC_CALLBACK_AUTH_KEY));
            }
            // warn about deprecation
        }
        var authCodeId = config.getString(EDC_CALLBACK_AUTH_CODE_ID, null);

        if (authCodeId == null) {
            authCodeId = config.getString(EDC_CALLBACK_AUTH_CODE_ID_LEGACY, null);

            if (authCodeId == null) {
                monitor.warning("Using deprecated configuration '%s'. Please switch to '%s'".formatted(EDC_CALLBACK_AUTH_CODE_ID_LEGACY, EDC_CALLBACK_AUTH_CODE_ID));
            }
        }


        if (authKey != null && authCodeId == null) {
            throw new EdcException(format("%s cannot be null if %s is present", EDC_CALLBACK_AUTH_CODE_ID_LEGACY, EDC_CALLBACK_AUTH_KEY_LEGACY));
        }

        return CallbackAddress.Builder.newInstance()
                .uri(config.getString(EDC_CALLBACK_URI))
                .transactional(config.getBoolean(EDC_CALLBACK_TRANSACTIONAL, false))
                .authKey(authKey)
                .authCodeId(authCodeId)
                .events(events)
                .build();
    }

}
