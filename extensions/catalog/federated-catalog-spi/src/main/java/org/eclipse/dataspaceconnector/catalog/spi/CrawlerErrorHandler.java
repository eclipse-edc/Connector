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

package org.eclipse.dataspaceconnector.catalog.spi;


import java.util.function.Consumer;

/**
 * Whenever a {@link Crawler} encounters an error, e.g. when a node is not reachable, a node does not respond within a given time frame,
 * or the response is invalid, it calls out to the {@code CrawlerErrorHandler} to signal that error.
 */
@FunctionalInterface
public interface CrawlerErrorHandler extends Consumer<WorkItem> {
}
