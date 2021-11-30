/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message;

import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MultipartRequest {

    private final Message header;
    private final String payload;
    private final VerificationResult verificationResult;

    private MultipartRequest(@NotNull Message header, @Nullable String payload, @Nullable VerificationResult verificationResult) {
        this.header = Objects.requireNonNull(header);
        this.payload = payload;
        this.verificationResult = verificationResult;
    }

    @NotNull
    public Message getHeader() {
        return header;
    }

    @Nullable
    public String getPayload() {
        return payload;
    }

    @Nullable
    public VerificationResult getVerificationResult() {
        return verificationResult;
    }

    public static class Builder {

        private Message header;
        private String payload;
        private VerificationResult verificationResult;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(@Nullable Message header) {
            this.header = header;
            return this;
        }

        public Builder payload(@Nullable String payload) {
            this.payload = payload;
            return this;
        }

        public Builder verificationResult(@Nullable VerificationResult verificationResult) {
            this.verificationResult = verificationResult;
            return this;
        }

        public MultipartRequest build() {
            return new MultipartRequest(header, payload, verificationResult);
        }
    }
}
