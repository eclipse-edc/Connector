/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */
package com.microsoft.dagx.util;

import com.microsoft.dagx.spi.DagxException;

/**
 * Denotes a cyclic dependency was encountered during runtime boot.
 */
public class CyclicDependencyException extends DagxException {

    CyclicDependencyException(Object item) {
        super("Cyclic extension dependency for " + item);
    }

    CyclicDependencyException(Object item, CyclicDependencyException e) {
        super("Cyclic extension dependency for " + item, e);
    }
}
