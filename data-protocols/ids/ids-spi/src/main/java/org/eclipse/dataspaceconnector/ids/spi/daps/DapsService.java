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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.daps;

import org.eclipse.dataspaceconnector.spi.Result;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;

/**
 * A Dynamic Attribute Provisioning Services as defined by IDS.
 */
public interface DapsService {

    /**
     * Verifies the token and returns a contained {@link ClaimToken} if valid.
     */
    Result<ClaimToken> verifyAndConvertToken(@Nullable String token);

}
