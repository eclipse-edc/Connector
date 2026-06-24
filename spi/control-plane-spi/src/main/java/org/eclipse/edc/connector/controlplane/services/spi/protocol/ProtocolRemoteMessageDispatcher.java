/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Dispatcher for sending dataspace protocol messages.
 */
public interface ProtocolRemoteMessageDispatcher {

    /**
     * Binds and sends the message.
     *
     * @param participantContextId the participant context id
     * @param responseType         the expected response type
     * @param message              the message
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T, M extends ProtocolRemoteMessage> CompletableFuture<StatusResult<T>> dispatch(String participantContextId, Class<T> responseType, M message);

    /**
     * Registers a message request factory and response parser
     *
     * @param <M>            the type of message
     * @param <RSP>          the response type
     * @param <REQ>          the request type
     * @param <RB>           the response body type
     * @param clazz          the message class.
     * @param requestFactory the request factory.
     * @param bodyExtractor  the body extractor function.
     */
    <M extends ProtocolRemoteMessage, RSP, REQ, RB> void registerMessage(Class<M> clazz,
                                                                         RequestFactory<M, REQ> requestFactory,
                                                                         ProtocolResponseBodyExtractor<RB, RSP> bodyExtractor);

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
