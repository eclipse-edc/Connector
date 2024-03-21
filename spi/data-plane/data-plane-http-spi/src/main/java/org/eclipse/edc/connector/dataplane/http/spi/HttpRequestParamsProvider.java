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

package org.eclipse.edc.connector.dataplane.http.spi;

import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * Permit to register {@link HttpRequestParams} decorators, that are used to enrich the HTTP request with
 * information taken from {@link DataFlowStartMessage}
 */
public interface HttpRequestParamsProvider {

    /**
     * Register source decorator
     */
    void registerSourceDecorator(HttpParamsDecorator decorator);

    /**
     * Register sink decorator
     */
    void registerSinkDecorator(HttpParamsDecorator decorator);

    /**
     * Provide HTTP request params for HttpDataSource
     */
    HttpRequestParams provideSourceParams(DataFlowStartMessage request);

    /**
     * Provide HTTP request params for HttpDataSink
     */
    HttpRequestParams provideSinkParams(DataFlowStartMessage request);
}
