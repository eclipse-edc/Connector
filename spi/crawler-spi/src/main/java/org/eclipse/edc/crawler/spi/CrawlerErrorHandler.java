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

import java.util.function.Consumer;

/**
 * Invoked whenever a {@link CrawlerAction} was completed unsuccessfully. Error handlers receive the entire {@link WorkItem}
 * to enable features like re-enqueueing it after a transient failure.
 * Inpsect {@link WorkItem#getErrors()} to obtain the actual errors.
 */
@FunctionalInterface
public interface CrawlerErrorHandler extends Consumer<WorkItem> {
}
