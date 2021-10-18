/*
 * Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Microsoft Corporation - initial API and implementation
 *
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class QueryResponse {
    private Status status;
    private List<String> errors = new ArrayList<>();
    private Stream<Asset> assets = Stream.empty();

    private QueryResponse(Status status) {
        this.status = status;
        errors = new ArrayList<>();
    }

    public QueryResponse() {

    }

    public static QueryResponse ok(Stream<Asset> result) {
        return Builder.newInstance()
                .status(Status.ACCEPTED)
                .assets(result)
                .build();
    }

    public Stream<Asset> getAssets() {
        return assets;
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public enum Status {
        ACCEPTED,
        NO_ADAPTER_FOUND
    }

    public static final class Builder {

        private final QueryResponse response;

        private Builder() {
            response = new QueryResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assets(Stream<Asset> assets) {
            this.response.assets = assets;
            return this;
        }

        public Builder status(Status status) {
            this.response.status = status;
            return this;
        }

        public QueryResponse build() {
            return response;
        }

        public Builder error(String error) {
            response.errors.add(error);
            return this;
        }
    }
}
