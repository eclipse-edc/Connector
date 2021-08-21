/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import java.util.Objects;

/**
 * Base Hub message request type.
 */
public abstract class AbstractHubRequest extends HubMessage {
    protected String iss;
    protected String aud;
    protected String sub;

    public String getIss() {
        return iss;
    }

    public String getAud() {
        return aud;
    }

    public String getSub() {
        return sub;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<T extends AbstractHubRequest, B extends Builder<T, B>> extends HubMessage.Builder {
        protected T request;

        public Builder<T, B> iss(String iss) {
            this.request.iss = iss;
            return this;
        }

        public B aud(String aud) {
            this.request.aud = aud;
            return (B) this;
        }

        public B sub(String sub) {
            this.request.sub = sub;
            return (B) this;
        }

        public void verify() {
            Objects.requireNonNull(request.iss, "iss");
            Objects.requireNonNull(request.aud, "aud");
            Objects.requireNonNull(request.sub, "sub");
        }

        protected Builder(T request) {
            this.request = request;
        }


    }
}
