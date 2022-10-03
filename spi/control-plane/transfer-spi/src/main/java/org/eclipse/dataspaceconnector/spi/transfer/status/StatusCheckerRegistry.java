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

package org.eclipse.dataspaceconnector.spi.transfer.status;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.jetbrains.annotations.Nullable;

/**
 * A registry of {@link StatusChecker}s. Status checkers are responsible for determining if a transfer process is complete.
 */
@ExtensionPoint
public interface StatusCheckerRegistry {

    /**
     * Registers a status checker for the destination type.
     */
    void register(String destinationType, StatusChecker statusChecker);

    /**
     * Registers a status checker for the destination type and if not found, returns null.
     */
    @Nullable
    StatusChecker resolve(String destinationType);
}
