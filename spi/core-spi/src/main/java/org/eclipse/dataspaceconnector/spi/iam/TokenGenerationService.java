/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface for generating token based on a set of claims.
 */
@FunctionalInterface
public interface TokenGenerationService {
    Result<TokenRepresentation> generate(@NotNull Map<String, Object> claims);
}
