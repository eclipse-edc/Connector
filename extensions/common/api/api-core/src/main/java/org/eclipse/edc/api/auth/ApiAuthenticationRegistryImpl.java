/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.auth;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ApiAuthenticationRegistryImpl implements ApiAuthenticationRegistry {

    private static final AuthenticationService ALL_PASS = new AllPassAuthenticationService();
    private final Map<String, AuthenticationService> services = new HashMap<>();

    public ApiAuthenticationRegistryImpl() {
    }

    @Override
    public void register(String context, AuthenticationService service) {
        services.put(context, service);
    }

    @Override
    public @NotNull AuthenticationService resolve(String context) {
        return services.getOrDefault(context, ALL_PASS);
    }
}
