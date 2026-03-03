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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.crawler.spi;

import org.eclipse.edc.crawler.spi.model.UpdateRequest;
import org.eclipse.edc.crawler.spi.model.UpdateResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Takes an {@link UpdateRequest}, sends it to the intended endpoint using a particular application protocol (e.g. HTTP) to get that
 * endpoint's data.
 * <p>
 * For example, one could devise a {@code WeatherForecastAction} that queries a weather forecast endpoint.
 */
public interface CrawlerAction extends Function<UpdateRequest, CompletableFuture<UpdateResponse>> {
}
