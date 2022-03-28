/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions;

/**
 * The {@link  InvalidCorrelationMessageException} is thrown when a correlation {@link de.fraunhofer.iais.eis.Message} is missing mandatory information.
 */
public class InvalidCorrelationMessageException extends RuntimeException {

    /**
     * {@link  InvalidCorrelationMessageException} indicating that the id of a {@link de.fraunhofer.iais.eis.Message} is null.
     */
    public static InvalidCorrelationMessageException createExceptionForCorrelationIdMissing() {
        return new InvalidCorrelationMessageException("Message ID of correlation message is null");
    }

    /**
     * {@link  InvalidCorrelationMessageException} indicating that the issuer of a {@link de.fraunhofer.iais.eis.Message} is null.
     */
    public static InvalidCorrelationMessageException createExceptionForIssuerConnectorMissing() {
        return new InvalidCorrelationMessageException("Issuer connector of correlation message is null");
    }

    /**
     * {@link  InvalidCorrelationMessageException} indicating that the sender agent of a {@link de.fraunhofer.iais.eis.Message} is null.
     */
    public static InvalidCorrelationMessageException createExceptionForSenderAgentMissing() {
        return new InvalidCorrelationMessageException("Sender agent of correlation message is null");
    }

    private InvalidCorrelationMessageException(String reason) {
        super(reason);
    }
}
