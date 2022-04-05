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

package org.eclipse.dataspaceconnector.dataplane.http;

import com.github.javafaker.Faker;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.Collections;
import java.util.Map;

import static okhttp3.Protocol.HTTP_2;

public class HttpTestFixtures {

    private static final Faker FAKER = new Faker();

    public static DataFlowRequest.Builder createRequest(String type) {
        return createRequest(
                Map.of(DataFlowRequestSchema.METHOD, "GET"),
                createDataAddress(type, Collections.emptyMap()).build(),
                createDataAddress(type, Collections.emptyMap()).build()
        );
    }

    public static DataFlowRequest.Builder createRequest(Map<String, String> properties, DataAddress source, DataAddress destination) {
        return DataFlowRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
                .properties(properties)
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .trackable(true);
    }

    public static DataAddress.Builder createDataAddress(String type, Map<String, String> properties) {
        return DataAddress.Builder.newInstance()
                .type(type)
                .properties(properties);
    }

    public static Response.Builder createHttpResponse() {
        var body = ResponseBody.create("{}", MediaType.get("application/json"));
        var request = new Request.Builder().url("https://test.com").build();
        return new Response.Builder().code(200).body(body).request(request).protocol(HTTP_2).message("");
    }

    private HttpTestFixtures() {
    }
}
