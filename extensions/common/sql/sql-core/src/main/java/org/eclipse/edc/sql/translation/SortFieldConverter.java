/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

/**
 * Converts a sort field in the canonical format to the sql representation.
 */
@FunctionalInterface
interface SortFieldConverter {

    /**
     * Converts the sort field into the SQL representation.
     *
     * @param sortField the sort field.
     * @return the SQL representation.
     */
    String convert(String sortField);
}
