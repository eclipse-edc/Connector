/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.store;

import org.eclipse.edc.spi.query.CriteriaToPredicate;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Default implementation of {@link QueryResolver} that applies query on a stream. Uses reflection to fetch object fields. Used in stores implementations.
 *
 * @param <T> type of the stream elements.
 */
public class ReflectionBasedQueryResolver<T> implements QueryResolver<T> {

    private final Class<T> typeParameterClass;
    private final CriteriaToPredicate<T> criteriaToPredicate;

    /**
     * Constructor for ReflectionBasedQueryResolver
     *
     * @param typeParameterClass        class of the type parameter. Used in reflection operation to recursively fetch a property from an object.
     * @param criterionOperatorRegistry converts from a criterion to a predicate
     */
    public ReflectionBasedQueryResolver(Class<T> typeParameterClass, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(typeParameterClass, new AndOperatorCriteriaToPredicate<>(criterionOperatorRegistry));
    }

    public ReflectionBasedQueryResolver(Class<T> typeParameterClass, CriteriaToPredicate<T> criteriaToPredicate) {
        this.typeParameterClass = typeParameterClass;
        this.criteriaToPredicate = criteriaToPredicate;
    }

    /**
     * Method to query a stream by provided specification.
     * Converts the criterion into 'and' predicate.
     * Applies sorting. When sort field is not found returns empty stream.
     * Applies offset and limit on the query result.
     *
     * @param stream      stream to be queried.
     * @param spec        query specification.
     * @return stream result from queries.
     */
    @Override
    public Stream<T> query(Stream<T> stream, QuerySpec spec) {
        var predicate = criteriaToPredicate.convert(spec.getFilterExpression());

        var filteredStream = stream.filter(predicate);

        var sortField = spec.getSortField();

        if (sortField != null) {
            if (ReflectionUtil.getFieldRecursive(typeParameterClass, sortField) == null) {
                throw new IllegalArgumentException(format("Cannot sort by %s, the field does not exist in %s", sortField, typeParameterClass));
            }

            var comparator = new FieldComparator<>(sortField, spec.getSortOrder());
            filteredStream = filteredStream.sorted(comparator);
        }

        return filteredStream.skip(spec.getOffset()).limit(spec.getLimit());
    }

}
