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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MultipartResponseFactory {

    private final MessageFactory messageFactory;

    public MultipartResponseFactory(@NotNull MessageFactory messageFactory) {
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    public MultipartResponse createBadParametersErrorMultipartResponse(@NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.badParameters(message))
                .build();
    }

    public MultipartResponse createNotFoundErrorMultipartResponse(@NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.rejectNotFound(message))
                .build();
    }

    public MultipartResponse createErrorMultipartResponse(@NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.messageTypeNotSupported(message))
                .build();
    }
}
