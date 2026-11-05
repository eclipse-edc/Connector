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

package org.eclipse.edc.crawler.spi.model;

import org.eclipse.edc.crawler.spi.CrawlerAction;

/**
 * {@link CrawlerAction}s accept {@code UpdateRequests} to execute. The {@code participantContextId} identifies the local
 * participant context on whose behalf the crawl is performed, so that the request is dispatched with that participant's
 * identity.
 */
public record UpdateRequest(String participantContextId, String nodeId, String nodeUrl, String protocol) {
}
