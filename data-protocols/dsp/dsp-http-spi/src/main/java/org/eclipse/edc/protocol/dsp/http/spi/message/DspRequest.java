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

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.ErrorMessage;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DspRequest<I, R, E extends ErrorMessage, C> {

    protected final Class<R> resultClass;
    protected final Class<I> inputClass;
    protected final Class<E> errorClass;

    protected String token;
    protected String protocol;
    protected BiFunction<I, TokenRepresentation, ServiceResult<R>> serviceCall;
    protected ServiceProtocolCall<I, R, C> serviceProtocolCall;
    protected Supplier<? extends ErrorMessage.Builder<E, ?>> errorProvider;

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

    public BiFunction<I, TokenRepresentation, ServiceResult<R>> getServiceCall() {
        return serviceCall;
    }

    public ServiceProtocolCall<I, R, C> getServiceProtocolCall() {
        return serviceProtocolCall;
    }

    public Supplier<? extends ErrorMessage.Builder<E, ?>> getErrorProvider() {
        return errorProvider;
    }

    public abstract static class Builder<I, R, M extends DspRequest<I, R, E, C>, E extends ErrorMessage, C, B extends Builder<I, R, M, E, C, B>> {

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

        public B serviceCall(BiFunction<I, TokenRepresentation, ServiceResult<R>> serviceCall) {
            message.serviceCall = serviceCall;
            return self();
        }

        public B serviceCall(ServiceProtocolCall<I, R, C> serviceProtocolCall) {
            message.serviceProtocolCall = serviceProtocolCall;
            return self();
        }

        public B errorProvider(Supplier<? extends ErrorMessage.Builder<E, ?>> errorProvider) {
            message.errorProvider = errorProvider;
            return self();
        }

        public M build() {
            if (message.serviceCall == null && message.serviceProtocolCall == null) {
                throw new NullPointerException("serviceCall and serviceProtocolCall cannot be both null");
            }
            requireNonNull(message.protocol);
            requireNonNull(message.errorProvider);
            return message;
        }

        protected abstract B self();

    }
}
