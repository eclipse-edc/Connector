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
 *       Daimler TSS GmbH - introduce factory to create IDS ResponseMessages
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.IdsResponseMessageFactory;
import org.jetbrains.annotations.NotNull;

class MultipartResponseUtil {

    public static MultipartResponse createBadParametersErrorMultipartResponse(@NotNull IdsResponseMessageFactory factory, @NotNull Message message) {
        RejectionMessage badParametersMessage = factory.createBadParametersMessage(message);
        return MultipartResponse.Builder.newInstance().header(badParametersMessage).build();
    }

    public static MultipartResponse createNotFoundErrorMultipartResponse(@NotNull IdsResponseMessageFactory factory, @NotNull Message message) {
        RejectionMessage notFoundMessage = factory.createNotFoundMessage(message);
        return MultipartResponse.Builder.newInstance().header(notFoundMessage).build();
    }

    public static MultipartResponse createErrorMultipartResponse(@NotNull IdsResponseMessageFactory factory, @NotNull Message message) {
        RejectionMessage messageTypeNotSupportedMessage = factory.createMessageTypeNotSupportedMessage(message);
        return MultipartResponse.Builder.newInstance().header(messageTypeNotSupportedMessage).build();
    }
}
