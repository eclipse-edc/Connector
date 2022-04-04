/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.provision.http;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.mockito.invocation.InvocationOnMock;

import java.util.Map;

import static okhttp3.Protocol.HTTP_1_1;

/**
 * Test configuration.
 */
public class HttpProvisionerFixtures {
    public static final String HTTP_PROVISIONER_ENTRIES = "provisioner.http.entries.";

    public static final String TEST_DATA_TYPE = "test-data-type";

    public static final Map<String, String> PROVISIONER_CONFIG = Map.of(
            HTTP_PROVISIONER_ENTRIES + "provisioner1.provisioner.type", "provider",
            HTTP_PROVISIONER_ENTRIES + "provisioner1.policy.scope", "provision1.scope",
            HTTP_PROVISIONER_ENTRIES + "provisioner1.endpoint", "http://foo.com",
            HTTP_PROVISIONER_ENTRIES + "provisioner1.data.address.type", TEST_DATA_TYPE
    );

    public static Response createResponse(int code, InvocationOnMock invocation) {
        Interceptor.Chain chain = invocation.getArgument(0);
        return new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(code)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("test")
                .build();
    }

    private HttpProvisionerFixtures() {
    }
}
