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

public class GetDspRequest<R> extends DspRequest<String, R> {

    private String id;

    private GetDspRequest(Class<R> resultClass) {
        super(String.class, resultClass);
    }

    public String getId() {
        return id;
    }

    public static class Builder<R> extends DspRequest.Builder<String, R, GetDspRequest<R>, Builder<R>> {

        public static <R> Builder<R> newInstance(Class<R> resultClass) {
            return new Builder<>(resultClass);
        }

        private Builder(Class<R> resultClass) {
            super(new GetDspRequest<>(resultClass));
        }

        public Builder<R> id(String id) {
            super.message.id = id;
            return this;
        }

        @Override
        protected Builder<R> self() {
            return this;
        }
    }
}
