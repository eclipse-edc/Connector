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

public class S3BucketSchema {

    private S3BucketSchema() {
    }

    public static final String TYPE = "AmazonS3";
    public static final String REGION = "region";
    public static final String BUCKET_NAME = "bucketName";
}
