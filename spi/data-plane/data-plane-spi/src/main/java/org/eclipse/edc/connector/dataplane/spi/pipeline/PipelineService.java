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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.pipeline;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Transfers data from a source to a sink.
 *
 * @deprecated this interface will be removed from the hierarchy, please use {@link TransferService} instead.
 */
@ExtensionPoint
@Deprecated(since = "0.3.0")
public interface PipelineService extends TransferService {

}
