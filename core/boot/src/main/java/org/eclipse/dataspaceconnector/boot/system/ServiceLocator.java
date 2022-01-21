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

package org.eclipse.dataspaceconnector.boot.system;

import java.util.List;

/**
 * Locates implementors of a given class.
 */
public interface ServiceLocator {
    /**
     * Locates all implementors/inheritors of a given abstract class or interface <code>type</code>. A EdcException is
     * thrown if implementors are required, but none are found
     *
     * @param type     The abstract class or interface whos implementors to find
     * @param required Whether implementors of <code>type</code> MUST exist. A EdcException is thrown if required is true, but none are found
     * @return A list of implementors or an empty list if none found (and required=false)
     */
    <T> List<T> loadImplementors(Class<T> type, boolean required);

    /**
     * Locates a single implementor/inheritor of a given abstract class or interface. A EdcException is thrown if either
     * none are found and <code>required=true</code>, or if > 1 implementors/inheritors are found.
     *
     * @param type     The abstract class or interface whos implementors to find
     * @param required Whether implementors of <code>type</code> MUST exist. A EdcException is thrown if required is true, but none are found
     * @return An implementor/inheritor of <code>type</code>
     */
    <T> T loadSingletonImplementor(Class<T> type, boolean required);
}
