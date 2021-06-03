/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

@FunctionalInterface
public interface CompletionChecker {
    boolean check();
}
