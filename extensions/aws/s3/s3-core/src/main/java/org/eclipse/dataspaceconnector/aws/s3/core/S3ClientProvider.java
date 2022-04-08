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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Provides S3Client
 */
@Feature("edc:aws:s3:client-provider")
public interface S3ClientProvider {

    /**
     * Returns the client for the specified secret token
     */
    S3Client provide(String region, SecretToken secretToken);

    /**
     * Returns the client for the specified aws credential
     */
    S3Client provide(String region, AwsCredentials credentials);

}
