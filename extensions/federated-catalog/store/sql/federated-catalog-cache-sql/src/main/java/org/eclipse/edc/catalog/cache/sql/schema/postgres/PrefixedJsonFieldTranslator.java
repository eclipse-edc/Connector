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

package org.eclipse.edc.catalog.cache.sql.schema.postgres;

import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.List;
import java.util.stream.Stream;

/**
 * An extension of {@link JsonFieldTranslator} where the list of path is prefixed with
 * the provided prefix value. This is useful for mapping field that are stored in a single json column
 * without exposing the column/path name directly. For example, we store the catalog DTO as JSON inside the `catalog` column.
 * and we want to expose the catalog fields directly without using the prefix `catalog`. For query like
 * properties.name = 'name' where the `properties` field is stored inside the json column `catalog` and thus
 * the path `catalog.properties` the final JSON filter should be `catalog -> properties ->> name. With only {@link JsonFieldTranslator}
 * we would get `catalog ->> name`.
 */
public class PrefixedJsonFieldTranslator extends JsonFieldTranslator {

    private final String prefix;

    public PrefixedJsonFieldTranslator(String columnName, String prefix) {
        super(columnName);
        this.prefix = prefix;
    }

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> type) {
        return super.getLeftOperand(Stream.concat(PathItem.parse(prefix).stream(), path.stream()).toList(), type);
    }
}
