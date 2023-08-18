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

package org.eclipse.edc.protocol.dsp.spi.message;

import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

public class DspRequest<I, R> {

    protected final Class<R> resultClass;
    protected final Class<I> inputClass;
    protected String token;
    protected String errorType;
    protected BiFunction<I, ClaimToken, ServiceResult<R>> serviceCall;

    public DspRequest(Class<I> inputClass, Class<R> resultClass) {
        this.inputClass = inputClass;
        this.resultClass = resultClass;
    }

    public String getToken() {
        return token;
    }

    public Class<I> getInputClass() {
        return inputClass;
    }

    public Class<R> getResultClass() {
        return resultClass;
    }

    public BiFunction<I, ClaimToken, ServiceResult<R>> getServiceCall() {
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

        public B serviceCall(BiFunction<I, ClaimToken, ServiceResult<R>> serviceCall) {
            message.serviceCall = serviceCall;
            return self();
        }

        public B errorType(String errorType) {
            message.errorType = errorType;
            return self();
        }

        public M build() {
            requireNonNull(message.token);
            requireNonNull(message.serviceCall);
            requireNonNull(message.errorType);
            return message;
        }

        protected abstract B self();

    }
}
