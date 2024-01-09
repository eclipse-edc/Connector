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

package org.eclipse.edc.token.spi;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record KeyIdDecorator(@Nullable String keyId) implements JwtDecorator {
    @Override
    public Map<String, Object> headers() {
        if (keyId != null) {
            return Map.of("kid", keyId); //will throw an exception if keyId is null
        }
        return Map.of();
    }
}
