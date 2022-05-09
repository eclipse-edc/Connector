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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.azure.core.credential.AzureSasCredential;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;

class TestFunctions {
    static AzureSasCredential sharedAccessSignatureMatcher(String sharedAccessSignature) {
        return argThat((AzureSasCredential c) -> Objects.equals(c.getSignature(), sharedAccessSignature));
    }
}