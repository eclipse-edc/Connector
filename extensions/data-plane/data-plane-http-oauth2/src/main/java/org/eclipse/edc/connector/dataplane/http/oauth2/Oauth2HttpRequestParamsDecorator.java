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

package org.eclipse.edc.connector.dataplane.http.oauth2;

import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

class Oauth2HttpRequestParamsDecorator implements HttpParamsDecorator {

    private final Oauth2CredentialsRequestFactory requestFactory;
    private final Oauth2Client client;

    public Oauth2HttpRequestParamsDecorator(Oauth2CredentialsRequestFactory requestFactory, Oauth2Client client) {
        this.requestFactory = requestFactory;
        this.client = client;
    }

    @Override
    public HttpRequestParams.Builder decorate(DataFlowRequest request, HttpDataAddress address, HttpRequestParams.Builder params) {
        return requestFactory.create(address)
                .compose(client::requestToken)
                .map(tokenRepresentation -> params.header("Authorization", "Bearer " + tokenRepresentation.getToken()))
                .orElseThrow(failure -> new EdcException("Cannot authenticate through OAuth2: " + failure.getFailureDetail()));
    }
}
