/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.spi.entity;

/**
 * Function that returns the int status representation given the String one
 */
@FunctionalInterface
public interface StateResolver {

    /**
     * Returns the int state giving its String representation.
     *
     * @param stringState string state representation.
     * @return the int state.
     */
    int resolve(String stringState);

}
