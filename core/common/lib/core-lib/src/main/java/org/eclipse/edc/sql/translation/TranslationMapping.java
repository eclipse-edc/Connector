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

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@linkplain TranslationMapping} keeps a tree of {@link FieldTranslator}s that can be obtained using canonical
 * information about business objects to SQL, i.e. it contains field names of an object and maps them to their SQL schema
 * column name.
 * <p>
 * Essentially, it contains a map that links field name to column name. In case the field is a complex object, it in
 * turn must be represented by another {@link TranslationMapping} in order to recursively resolve the field translator.
 */
public abstract class TranslationMapping {

    private final Map<String, Object> fieldMap = new HashMap<>();

    /**
     * Returns the {@link FieldTranslator} for the specified path.
     *
     * @param fieldPath the path name.
     * @return the {@link FieldTranslator}, or null if it does not exist.
     */
    public Function<Class<?>, String> getFieldTranslator(String fieldPath) {
        return getFieldTranslator(PathItem.parse(fieldPath));
    }

    /**
     * Returns the {@link WhereClause} for the specified criterion and operator.
     *
     * @param criterion the criterion.
     * @param operator the operator.
     * @return the {@link WhereClause}.
     */
    public WhereClause getWhereClause(Criterion criterion, SqlOperator operator) {
        var path = PathItem.parse(criterion.getOperandLeft().toString());
        return getWhereClause(path, criterion, operator);
    }

    /**
     * Add a simple column field translator.
     *
     * @param fieldPath the field path.
     * @param columnName the column name.
     */
    protected void add(String fieldPath, String columnName) {
        fieldMap.put(fieldPath, new PlainColumnFieldTranslator(columnName));
    }

    /**
     * Add a {@link FieldTranslator}.
     *
     * @param fieldPath the field path.
     * @param fieldTranslator the field translator.
     */
    protected void add(String fieldPath, FieldTranslator fieldTranslator) {
        fieldMap.put(fieldPath, fieldTranslator);
    }

    /**
     * Add a nested {@link TranslationMapping}.
     *
     * @param fieldPath the field path.
     * @param translationMapping the {@link TranslationMapping}.
     */
    protected void add(String fieldPath, TranslationMapping translationMapping) {
        fieldMap.put(fieldPath, translationMapping);
    }

    private Function<Class<?>, String> getFieldTranslator(List<PathItem> path) {
        var entry = fieldMap.get(path.get(0).toString());
        if (entry == null) {
            return null;
        }

        var nestedPath = path.stream().skip(1).toList();
        if (entry instanceof FieldTranslator fieldTranslator) {
            return clazz -> fieldTranslator.getLeftOperand(nestedPath, clazz);
        } else if (entry instanceof TranslationMapping mappingEntry) {
            return mappingEntry.getFieldTranslator(nestedPath);
        } else {
            throw new IllegalArgumentException("unexpected mapping");
        }
    }

    private WhereClause getWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator) {
        var entry = fieldMap.get(path.get(0).toString());
        if (entry == null) {
            return null;
        }

        if (entry instanceof FieldTranslator fieldTranslator) {
            var nestedPath = path.size() == 1 ? path : path.stream().skip(1).toList();
            return fieldTranslator.toWhereClause(nestedPath, criterion, operator);
        } else if (entry instanceof TranslationMapping mappingEntry) {
            var nestedPath = path.stream().skip(1).toList();
            return mappingEntry.getWhereClause(nestedPath, criterion, operator);
        } else {
            throw new IllegalArgumentException("unexpected mapping");
        }
    }

}
