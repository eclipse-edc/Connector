/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.message;

import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

@FunctionalInterface
public interface ServiceCall<I, R> {

    /**
     * Applies the service call with the given input and token representation.
     *
     * @param participantContext  the participant context
     * @param input               the input to the service call
     * @param tokenRepresentation the token representation for authentication/authorization
     * @return a ServiceResult containing either the result or an error
     */
    ServiceResult<R> apply(ParticipantContext participantContext, I input, TokenRepresentation tokenRepresentation);

}