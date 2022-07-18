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
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.createResponseMessageForStatusResult;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.internalRecipientError;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseMessageUtil.notFound;

/**
 * Provides utility methods for building IDS multipart responses for common responses.
 */
public class MultipartResponseUtil {
    
    /**
     * Creates a multipart response with a rejection message with reason BAD_PARAMETERS as header.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @return the multipart response.
     */
    public static MultipartResponse createBadParametersErrorMultipartResponse(@NotNull String connectorId, @NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }
    
    /**
     * Creates a multipart response with a rejection message with reason BAD_PARAMETERS as header
     * and the given payload.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @param payload the response payload.
     * @return the multipart response.
     */
    public static MultipartResponse createBadParametersErrorMultipartResponse(@NotNull String connectorId, @NotNull Message message, String payload) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .payload(payload)
                .build();
    }
    
    /**
     * Creates a multipart response with a rejection message with reason NOT_FOUND as header.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @return the multipart response.
     */
    public static MultipartResponse createNotFoundErrorMultipartResponse(@NotNull String connectorId, @NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(notFound(message, connectorId))
                .build();
    }
    
    /**
     * Creates a multipart response with a rejection message with reason INTERNAL_RECIPIENT_ERROR as header.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @return the multipart response.
     */
    public static MultipartResponse createInternalRecipientErrorMultipartResponse(@NotNull String connectorId, @NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(internalRecipientError(message, connectorId))
                .build();
    }
    
    /**
     * Creates a multipart response with a rejection message with reason MESSAGE_TYPE_NOT_SUPPORTED as header.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @return the multipart response.
     */
    public static MultipartResponse createMessageTypeNotSupportedErrorMultipartResponse(@NotNull String connectorId, @NotNull Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, connectorId))
                .build();
    }
    
    /**
     * Creates a multipart response from a status result. Will return a rejection message or a
     * RequestInProcessMessage depending on the result.
     *
     * @param connectorId the connector ID.
     * @param message the request.
     * @param result the status result.
     * @return the multipart response.
     */
    public static MultipartResponse createMultipartResponseFromStatusResult(@NotNull String connectorId, @NotNull Message message, @NotNull StatusResult<?> result) {
        return MultipartResponse.Builder.newInstance()
                .header(createResponseMessageForStatusResult(result, message, connectorId))
                .build();
    }
}
