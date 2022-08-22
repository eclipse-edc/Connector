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

package org.eclipse.dataspaceconnector.core;

import org.eclipse.dataspaceconnector.junit.extensions.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import static org.mockito.Mockito.spy;

// needed for one particular test, to be able to verify on the TypeManager.
public class TypeManagerDependencyInjectionExtension extends DependencyInjectionExtension {
    @Override
    protected @NotNull TypeManager createTypeManager() {
        return spy(new TypeManager());
    }
}
