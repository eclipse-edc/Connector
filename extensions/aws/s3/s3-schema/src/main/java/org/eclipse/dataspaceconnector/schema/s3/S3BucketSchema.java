/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.schema.s3;


import org.eclipse.dataspaceconnector.schema.DataSchema;
import org.eclipse.dataspaceconnector.schema.SchemaAttribute;

public class S3BucketSchema extends DataSchema {

    public static final String TYPE = "AmazonS3";
    public static String REGION = "region";
    public static String BUCKET_NAME = "bucketName";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("region", true));
        attributes.add(new SchemaAttribute("bucketName", true));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
