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

package org.eclipse.edc.connector.api.management.configuration.transform;

import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

public class ManagementApiTypeTransformerRegistryImpl extends TypeTransformerRegistryImpl implements ManagementApiTypeTransformerRegistry {

    private final TypeTransformerRegistry fallback;

    public ManagementApiTypeTransformerRegistryImpl(TypeTransformerRegistry fallback) {
        super();
        this.fallback = fallback;
    }

    @Override
    public @NotNull <INPUT, OUTPUT> TypeTransformer<INPUT, OUTPUT> transformerFor(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        try {
            return super.transformerFor(input, outputType);
        } catch (EdcException exception) {
            return fallback.transformerFor(input, outputType);
        }
    }
}
