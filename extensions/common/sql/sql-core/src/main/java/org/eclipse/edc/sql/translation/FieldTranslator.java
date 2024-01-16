/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.spi.types.PathItem;

import java.util.List;

/**
 * Component that can translate a canonical field path into a sql column name or path.
 */
public interface FieldTranslator {

    /**
     * Get left operand for a SQL query given the canonical path name and the type
     *
     * @param pathName the path name.
     * @param type the type.
     * @return the left operand.
     */
    String getLeftOperand(List<PathItem> path, Class<?> type);

}
