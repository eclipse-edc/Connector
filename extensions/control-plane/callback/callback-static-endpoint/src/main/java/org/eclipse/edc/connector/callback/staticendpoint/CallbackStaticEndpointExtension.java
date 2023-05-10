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

package org.eclipse.edc.connector.callback.staticendpoint;

import org.eclipse.edc.connector.spi.callback.CallbackRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
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
    public static final String EDC_CALLBACK_URI = "uri";
    public static final String EDC_CALLBACK_EVENTS = "events";
    public static final String EDC_CALLBACK_TRANSACTIONAL = "transactional";
    public static final String EDC_CALLBACK_AUTH_KEY = "auth-key";
    public static final String EDC_CALLBACK_AUTH_CODE_ID = "auth-code-id";
    static final String NAME = "Static callbacks extension";
    @Inject
    private CallbackRegistry callbackRegistry;

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
        var authCodeId = config.getString(EDC_CALLBACK_AUTH_CODE_ID, null);

        if (authKey != null && authCodeId == null) {
            throw new EdcException(format("%s cannot be null if %s is present", EDC_CALLBACK_AUTH_CODE_ID, EDC_CALLBACK_AUTH_KEY));
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
