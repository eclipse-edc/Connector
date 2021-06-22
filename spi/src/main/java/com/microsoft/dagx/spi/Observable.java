/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi;

public interface Observable<T> {
    void unregister(T listener);
}
