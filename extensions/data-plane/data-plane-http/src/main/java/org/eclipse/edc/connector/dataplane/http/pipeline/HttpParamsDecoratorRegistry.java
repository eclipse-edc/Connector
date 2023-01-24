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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

/**
 * Permits to register {@link HttpRequestParams} decorators, that are used to enrich the HTTP request with
 * information taken from {@link DataFlowRequest}
 */
@ExtensionPoint
public interface HttpParamsDecoratorRegistry {

    /**
     * Register source decorator
     */
    void registerSourceDecorator(HttpParamsDecorator decorator);

    /**
     * Register sink decorator
     */
    void registerSinkDecorator(HttpParamsDecorator decorator);

    /**
     * Decorate http request params builder on the source side
     */
    void decorateSource(DataFlowRequest request, HttpRequestParams.Builder builder);

    /**
     * Decorate http request params builder on the sink side
     */
    void decorateSink(DataFlowRequest request, HttpRequestParams.Builder builder);
}
