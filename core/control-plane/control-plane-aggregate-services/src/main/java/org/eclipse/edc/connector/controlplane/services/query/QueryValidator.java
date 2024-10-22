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

package org.eclipse.edc.connector.controlplane.services.query;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

/**
 * Validates that a particular query is valid, i.e. contains a left-hand operand, that conforms to the canonical
 * format.
 */
public class QueryValidator {
    private final Class<?> canonicalType;
    private final Map<Class<?>, List<Class<?>>> subtypeMap;

    /**
     * Constructs a new QueryValidator instance.
     *
     * @param canonicalType    The Java class of the object to validate against.
     * @param typeHierarchyMap Contains mapping from superclass to list of subclasses. Every superclass must be
     *                         represented as separate entry in the map, even if it is also a subclass of another.
     */
    public QueryValidator(Class<?> canonicalType, Map<Class<?>, List<Class<?>>> typeHierarchyMap) {
        this.canonicalType = canonicalType;
        subtypeMap = typeHierarchyMap;
    }

    public QueryValidator(Class<?> canonicalType) {
        this(canonicalType, emptyMap());
    }

    /**
     * Validates a {@link QuerySpec} whether it conforms to a particular schema (e.g. a Java class) or not
     */
    public Result<Void> validate(QuerySpec query) {
        return query.getFilterExpression().stream()
                .map(Criterion::getOperandLeft)
                .map(Object::toString)
                .map(this::isValid)
                .reduce(Result::merge)
                .orElse(Result.success());
    }

    /**
     * Decide whether a particular "path" (i.e. a Criterion's left-hand operand) is valid or not. Traverses through the
     * object graph recursively and matches each path token to a {@link Field}. If none is found a failure is returned.
     *
     * @param path The path. Cannot start or end with a "."
     */
    protected Result<Void> isValid(String path) {
        if (path.endsWith(".") || path.startsWith(".")) {
            return Result.failure("Invalid path expression");
        }
        var pathTokens = path.split("\\.");

        var type = canonicalType;

        for (var token : pathTokens) {

            // cannot query on extensible (=Map) types
            if (type == Map.class) {
                var pattern = Pattern.compile("^[0-9A-Za-z.':/@]*$");
                var matcher = pattern.matcher(path);
                return matcher.find() ? Result.success() :
                        Result.failure("Querying Map types is not yet supported");
            }
            var field = getFieldIncludingSubtypes(type, token);
            if (field != null) {
                type = field.getType();
                if (Collection.class.isAssignableFrom(type)) {
                    var genericType = (ParameterizedType) field.getGenericType();
                    type = (Class<?>) genericType.getActualTypeArguments()[0];
                }
            } else {
                return Result.failure(format("Field %s not found on type %s", token, type));
            }
        }
        return Result.success();
    }

    private Field getFieldIncludingSubtypes(Class<?> type, String token) {
        var field = ReflectionUtil.getFieldRecursive(type, token);
        if (field == null) {
            var subTypes = subtypeMap.get(type);
            if (subTypes != null) {
                return subTypes.stream().map(st -> getFieldIncludingSubtypes(st, token))
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
        }
        return field;
    }
}
