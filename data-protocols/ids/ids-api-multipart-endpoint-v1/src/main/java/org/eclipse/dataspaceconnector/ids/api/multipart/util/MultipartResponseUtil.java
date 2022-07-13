/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.notFound;

public class MultipartResponseUtil {

    public static MultipartResponse createBadParametersErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }
    
    public static MultipartResponse createBadParametersErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message, String payload) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .payload(payload)
                .build();
    }

    public static MultipartResponse createNotFoundErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(notFound(message, connectorId))
                .build();
    }
}
