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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class S3BucketProvisionedResourceTest {

    private S3BucketProvisionedResource provisionedResource;

    @Test
    void verifyDeserialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, provisionedResource);

        S3BucketProvisionedResource deserialized = mapper.readValue(writer.toString(), S3BucketProvisionedResource.class);

        assertNotNull(deserialized);
        assertEquals("region", deserialized.getRegion());
        assertEquals("bucket", deserialized.getBucketName());
    }

    @BeforeEach
    void setUp() {
        provisionedResource = S3BucketProvisionedResource.Builder.newInstance()
                .id(randomUUID().toString()).transferProcessId("123").resourceDefinitionId(randomUUID().toString()).region("region").bucketName("bucket").build();
    }
}
