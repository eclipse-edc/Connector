/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.Objects;

public class TransferType {
    private String contentType;
    private boolean isFinite = true;

    public String getContentType() {
        return contentType;
    }

    public boolean isFinite() {
        return isFinite;
    }


    public static final class Builder {
        private String contentType = "application/octet-stream";
        private boolean isFinite = true;

        private Builder() {
        }

        public static Builder transferType() {
            return new Builder();
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder isFinite(boolean isFinite) {
            this.isFinite = isFinite;
            return this;
        }

        public TransferType build() {
            TransferType transferType = new TransferType();
            transferType.contentType = Objects.requireNonNull(contentType, "Content type cannot be null!");
            transferType.isFinite = isFinite;
            return transferType;
        }
    }
}
