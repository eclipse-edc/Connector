/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.lib.iam;

import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;

import java.io.IOException;

/**
 * Factory for creating iam clients. Uses application-default credentials
 */
public class IamClientFactory {
    public IamClientFactory() {
    }

    public IAMClient createIamClient() throws IOException {
        return IAMClient.create();
    }

    public IamCredentialsClient createCredentialsClient() throws IOException {
        return IamCredentialsClient.create();
    }
}

