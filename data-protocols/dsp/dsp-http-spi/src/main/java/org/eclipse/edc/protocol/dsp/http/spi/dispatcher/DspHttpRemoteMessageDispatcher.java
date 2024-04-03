/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.dispatcher;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.function.Function;

/**
 * {@link RemoteMessageDispatcher} for sending dataspace protocol messages.
 */
public interface DspHttpRemoteMessageDispatcher extends RemoteMessageDispatcher {

    /**
     * Registers a message request factory and response parser
     *
     * @param <M>            the type of message
     * @param <R>            the response type
     * @param clazz          the message class.
     * @param requestFactory the request factory.
     * @param bodyExtractor  the body extractor function.
     */
    <M extends RemoteMessage, R> void registerMessage(Class<M> clazz, DspHttpRequestFactory<M> requestFactory,
                                                      DspHttpResponseBodyExtractor<R> bodyExtractor);

    /**
     * Registers a {@link Policy} scope to be evaluated for certain types of messages
     *
     * @param messageClass the message type for which evaluate the policy.
     * @param scope the scope to be used.
     * @param policyProvider function that extracts the Policy from the message.
     * @param <M> the message type.
     */
    <M extends RemoteMessage> void registerPolicyScope(Class<M> messageClass, String scope, Function<M, Policy> policyProvider);
}
