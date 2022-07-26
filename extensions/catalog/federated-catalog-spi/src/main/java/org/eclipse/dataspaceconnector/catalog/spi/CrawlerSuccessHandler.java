/*
 *  Copyright (c) 2022 Microsoft Corporation
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

import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.function.Consumer;

/**
 * Handler to be called when a crawler has successfully queried a catalog node
 */
@FunctionalInterface
public interface CrawlerSuccessHandler extends Consumer<UpdateResponse> {
}
