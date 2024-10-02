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

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

public class DspRequest<I, R> {

    protected final Class<R> resultClass;
    protected final Class<I> inputClass;
    protected String token;
    protected String errorType;
    protected String protocol;
    protected BiFunction<I, TokenRepresentation, ServiceResult<R>> serviceCall;

    public DspRequest(Class<I> inputClass, Class<R> resultClass) {
        this.inputClass = inputClass;
        this.resultClass = resultClass;
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

    public String getErrorType() {
        return errorType;
    }

    public abstract static class Builder<I, R, M extends DspRequest<I, R>, B extends Builder<I, R, M, B>> {

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

        public B errorType(String errorType) {
            message.errorType = errorType;
            return self();
        }

        public M build() {
            requireNonNull(message.serviceCall);
            requireNonNull(message.errorType);
            requireNonNull(message.protocol);
            return message;
        }

        protected abstract B self();

    }
}
