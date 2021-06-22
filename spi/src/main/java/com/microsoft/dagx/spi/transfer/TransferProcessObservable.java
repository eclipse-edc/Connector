/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.Observable;

public interface TransferProcessObservable extends Observable<TransferProcessListener> {
    void registerListener(String processId, TransferProcessListener listener);
}
