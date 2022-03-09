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

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Provides S3Client
 */
public interface S3ClientProvider {

    /**
     * Returns the client for the specified region and secretToken
     */
    S3Client provide(String region, SecretToken secretToken);

}
