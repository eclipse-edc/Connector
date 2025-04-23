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
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.message;

import org.eclipse.edc.spi.types.domain.message.ErrorMessage;

public class GetDspRequest<R, E extends ErrorMessage> extends DspRequest<String, R, E> {

    private String id;
    private boolean authRequired = true;

    private GetDspRequest(Class<R> resultClass, Class<E> errorClass) {
        super(String.class, resultClass, errorClass);
    }

    public String getId() {
        return id;
    }
    
    public boolean isAuthRequired() {
        return authRequired;
    }

    public static class Builder<R, E extends ErrorMessage> extends DspRequest.Builder<String, R, GetDspRequest<R, E>, E, Builder<R, E>> {

        private Builder(Class<R> resultClass, Class<E> errorClass) {
            super(new GetDspRequest<>(resultClass, errorClass));
        }

        public static <R, E extends ErrorMessage> Builder<R, E> newInstance(Class<R> resultClass, Class<E> errorClass) {
            return new Builder<>(resultClass, errorClass);
        }

        public Builder<R, E> id(String id) {
            super.message.id = id;
            return this;
        }
        
        public Builder<R, E> authRequired(boolean authRequired) {
            super.message.authRequired = authRequired;
            return this;
        }

        @Override
        protected Builder<R, E> self() {
            return this;
        }
    }
}
