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

package org.eclipse.edc.connector.dataplane.http.params;

import org.eclipse.edc.connector.dataplane.http.params.decorators.BaseCommonHttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.params.decorators.BaseSinkHttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.params.decorators.BaseSourceHttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestParamsProviderImpl implements HttpRequestParamsProvider {

    private final List<HttpParamsDecorator> sourceDecorators = new ArrayList<>();
    private final List<HttpParamsDecorator> sinkDecorators = new ArrayList<>();

    public HttpRequestParamsProviderImpl(Vault vault, TypeManager typeManager) {
        var commonHttpParamsDecorator = new BaseCommonHttpParamsDecorator(vault, typeManager);
        registerSinkDecorator(commonHttpParamsDecorator);
        registerSourceDecorator(commonHttpParamsDecorator);
        registerSourceDecorator(new BaseSourceHttpParamsDecorator());
        registerSinkDecorator(new BaseSinkHttpParamsDecorator());
    }

    @Override
    public void registerSourceDecorator(HttpParamsDecorator decorator) {
        sourceDecorators.add(decorator);
    }

    @Override
    public void registerSinkDecorator(HttpParamsDecorator decorator) {
        sinkDecorators.add(decorator);
    }

    @Override
    public HttpRequestParams provideSourceParams(DataFlowStartMessage request) {
        var params = HttpRequestParams.Builder.newInstance();
        var address = HttpDataAddress.Builder.newInstance().copyFrom(request.getSourceDataAddress()).build();
        sourceDecorators.forEach(decorator -> decorator.decorate(request, address, params));
        return params.build();
    }

    @Override
    public HttpRequestParams provideSinkParams(DataFlowStartMessage request) {
        var params = HttpRequestParams.Builder.newInstance();
        var address = HttpDataAddress.Builder.newInstance().copyFrom(request.getDestinationDataAddress()).build();
        sinkDecorators.forEach(decorator -> decorator.decorate(request, address, params));
        return params.build();
    }

}
