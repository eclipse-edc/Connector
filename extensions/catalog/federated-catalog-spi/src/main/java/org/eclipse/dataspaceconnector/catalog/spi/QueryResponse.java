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

package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.ArrayList;
import java.util.List;

public class QueryResponse {
    private Status status;
    private List<String> errors = new ArrayList<>();
    private List<ContractOffer> offers = new ArrayList<>();

    private QueryResponse(Status status) {
        this.status = status;
        errors = new ArrayList<>();
    }

    public QueryResponse() {

    }

    public static QueryResponse ok(List<ContractOffer> result) {
        return Builder.newInstance()
                .status(Status.ACCEPTED)
                .offers(result)
                .build();
    }

    public List<ContractOffer> getOffers() {
        return offers;
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
            response.status = Status.ACCEPTED; //thats the default
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offers(List<ContractOffer> assets) {
            response.offers = assets;
            return this;
        }

        public Builder status(Status status) {
            response.status = status;
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
