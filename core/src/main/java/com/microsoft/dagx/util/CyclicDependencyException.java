//
//  Code based on TopologicalSort from Eclipse Jetty licensed under Apache 2.0 (https://www.eclipse.org/jetty/).
//
//  Original license notice:
//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
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
