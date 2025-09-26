/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.message;

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.types.domain.message.ErrorMessage;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DspRequest<I, R, E extends ErrorMessage> {

    protected final Class<R> resultClass;
    protected final Class<I> inputClass;
    protected final Class<E> errorClass;

    protected String token;
    protected String protocol;
    protected ServiceCall<I, R> serviceCall;
    protected Supplier<? extends ErrorMessage.Builder<E, ?>> errorProvider;
    protected ParticipantContextSupplier participantContextProvider;

    public DspRequest(Class<I> inputClass, Class<R> resultClass, Class<E> errorClass) {
        this.inputClass = inputClass;
        this.resultClass = resultClass;
        this.errorClass = errorClass;
    }

    public String getToken() {
        return token;
    }

    public String getProtocol() {
        return protocol;
    }

    public Class<I> getInputClass() {
        return inputClass;
    }

    public Class<R> getResultClass() {
        return resultClass;
    }

    public ServiceCall<I, R> getServiceCall() {
        return serviceCall;
    }

    public Supplier<? extends ErrorMessage.Builder<E, ?>> getErrorProvider() {
        return errorProvider;
    }

    public Supplier<ParticipantContext> getParticipantContextProvider() {
        return participantContextProvider;
    }

    public abstract static class Builder<I, R, M extends DspRequest<I, R, E>, E extends ErrorMessage, B extends Builder<I, R, M, E, B>> {

        protected final M message;

        protected Builder(M message) {
            this.message = message;
        }

        public B token(String token) {
            message.token = token;
            return self();
        }

        public B protocol(String protocol) {
            message.protocol = protocol;
            return self();
        }

        public B serviceCall(ServiceCall<I, R> serviceCall) {
            message.serviceCall = serviceCall;
            return self();
        }

        public B participantContextProvider(ParticipantContextSupplier participantContextProvider) {
            message.participantContextProvider = participantContextProvider;
            return self();
        }

        public B errorProvider(Supplier<? extends ErrorMessage.Builder<E, ?>> errorProvider) {
            message.errorProvider = errorProvider;
            return self();
        }

        public M build() {
            requireNonNull(message.serviceCall);
            requireNonNull(message.protocol);
            requireNonNull(message.errorProvider);
            requireNonNull(message.participantContextProvider);
            return message;
        }

        protected abstract B self();

    }
}
