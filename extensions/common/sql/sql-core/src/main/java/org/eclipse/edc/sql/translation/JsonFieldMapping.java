/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.types.PathItem;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.IntStream.range;

public class JsonFieldMapping extends TranslationMapping {
    protected final String columnName;

    public JsonFieldMapping(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getStatement(List<PathItem> path, Class<?> type) {
        var statementBuilder = new StringBuilder(columnName);

        var length = path.size();
        range(0, length - 1)
                .mapToObj(i -> " -> '%s'".formatted(path.get(i)))
                .forEach(statementBuilder::append);

        statementBuilder.append(" ->> '%s'".formatted(path.get(length - 1)));
        var statement = statementBuilder.toString();

        if (type.equals(Boolean.class)) {
            return format("(%s)::boolean", statement);
        }

        return statement;
    }


}
