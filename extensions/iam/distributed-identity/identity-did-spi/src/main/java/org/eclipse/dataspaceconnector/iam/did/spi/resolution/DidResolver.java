/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a DID against an external resolver service.
 */
public interface DidResolver {

    /**
     * Returns the DID method this resolver supports.
     */
    @NotNull
    String getMethod();

    /**
     * Resolves the DID document or returns null if not found.
     */
    @Nullable
    DidDocument resolve(String didKey);

}
