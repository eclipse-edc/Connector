/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.dispatcher;

import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;

import java.util.function.Function;

/**
 * {@link RemoteMessageDispatcher} for sending dataspace protocol messages.
 */
public interface DspHttpRemoteMessageDispatcher extends RemoteMessageDispatcher<ProtocolRemoteMessage> {

    /**
     * Registers a message request factory and response parser
     *
     * @param <M>            the type of message
     * @param <R>            the response type
     * @param clazz          the message class.
     * @param requestFactory the request factory.
     * @param bodyExtractor  the body extractor function.
     */
    <M extends ProtocolRemoteMessage, R> void registerMessage(Class<M> clazz, DspHttpRequestFactory<M> requestFactory,
                                                      DspHttpResponseBodyExtractor<R> bodyExtractor);

    /**
     * Registers a {@link Policy} scope to be evaluated for certain types of messages
     *
     * @param <M>            the message type.
     * @param messageClass   the message type for which evaluate the policy.
     * @param policyProvider function that extracts the Policy from the message.
     */
    <M extends ProtocolRemoteMessage> void registerPolicyScope(Class<M> messageClass, Function<M, Policy> policyProvider,
                                                       RequestPolicyContext.Provider contextProvider);
}
