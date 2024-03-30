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

package org.eclipse.edc.connector.controlplane.defaults.callback;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CallbackRegistryImpl implements CallbackRegistry {

    private final List<CallbackAddress> callbackAddresses = new ArrayList<>();

    @Override
    public void register(CallbackAddress address) {
        callbackAddresses.add(address);
    }

    @Override
    public List<CallbackAddress> resolve(String eventName) {
        return callbackAddresses.stream()
                .filter(callbackAddress -> callbackAddress.getEvents().stream().anyMatch(eventName::startsWith))
                .collect(Collectors.toList());
    }
}
