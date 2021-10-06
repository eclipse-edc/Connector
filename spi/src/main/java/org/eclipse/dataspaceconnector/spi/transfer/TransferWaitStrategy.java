/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer;

/**
 * Implements a wait strategy for the {@link TransferProcessManager}.
 * <br/>
 * Implementations may choose to to enforce an incremental backoff period when successive errors are encountered.
 */
@FunctionalInterface
public interface TransferWaitStrategy extends WaitStrategy {


}
