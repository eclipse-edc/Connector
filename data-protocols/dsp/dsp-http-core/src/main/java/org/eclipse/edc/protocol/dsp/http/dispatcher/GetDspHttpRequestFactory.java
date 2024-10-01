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

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import okhttp3.HttpUrl;
import okhttp3.Request;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersion;
import org.eclipse.edc.protocol.dsp.http.spi.DspProtocolParser;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRequestFactory;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.RequestPathProvider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Creates a GET request through the Dataspace Protocol
 *
 * @param <M> the message class.
 */
public class GetDspHttpRequestFactory<M extends RemoteMessage> implements DspHttpRequestFactory<M> {
    private final RequestPathProvider<M> pathProvider;

    private final DspProtocolParser protocolParser;

    public GetDspHttpRequestFactory(DspProtocolParser protocolParser, RequestPathProvider<M> pathProvider) {
        this.protocolParser = protocolParser;
        this.pathProvider = pathProvider;
    }

    @Override
    public Request createRequest(M message) {

        var protocolPath = protocolParser.parse(message.getProtocol())
                .map(ProtocolVersion::path)
                .map(this::removeTrailingSlash)
                .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));

        var url = HttpUrl.get(message.getCounterPartyAddress() + protocolPath + pathProvider.providePath(message));

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
