/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */
package org.eclipse.dataspaceconnector.transfer.core.command;

import org.eclipse.dataspaceconnector.core.base.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommandQueue;

/**
 * In-memory implementation of the {@link CommandQueue} that is backed by a bounded queue. This means that attempting
 * to add elements to an already full queue will fail and raise an {@link IllegalStateException}.
 * <p>
 * This queue is threadsafe, so every operation on it can be assumed atomic.
 */
public class BoundedTransferProcessCommandQueue extends BoundedCommandQueue<TransferProcessCommand> implements TransferProcessCommandQueue {
    
    public BoundedTransferProcessCommandQueue(int bound) {
        super(bound);
    }
    
}
