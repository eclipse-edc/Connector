/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.message;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Represent a protocol service call.
 *
 * @param <I> input type.
 * @param <R> result type.
 * @param <C> context type.
 */
public interface ServiceProtocolCall<I, R, C>  {

    /**
     * Do the protocol service call.
     *
     * @param input the input.
     * @param participantAgent the participant agent
     * @param context the context.
     * @return the service call result.
     */
    ServiceResult<R> call(I input, ParticipantAgent participantAgent, C context);
}
