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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public interface WorkItemQueue extends BlockingQueue<WorkItem> {
    void lock();

    void unlock();

    boolean tryLock(long timeout, TimeUnit unit);
}
