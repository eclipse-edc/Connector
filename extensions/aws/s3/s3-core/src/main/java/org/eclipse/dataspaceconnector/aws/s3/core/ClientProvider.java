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

package org.eclipse.dataspaceconnector.aws.s3.core;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import software.amazon.awssdk.core.SdkClient;

/**
 * Provides reusable S3 clients that are configured to connect to specific regions and endpoints.
 */
@Feature("edc:aws:clientprovider")
public interface ClientProvider {

    /**
     * Returns the client of the specified type for the region id or endpoint URI.
     */
    <T extends SdkClient> T clientFor(Class<T> type, String key);

}
