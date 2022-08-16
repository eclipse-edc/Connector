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

package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil.propertyComparator;

/**
 * Default implementation of {@link QueryResolver} that applies query on a stream. Uses reflection to fetch object fields. Used in stores implementations.
 *
 * @param <T> type of the stream elements.
 */
public class ReflectionBasedQueryResolver<T> implements QueryResolver<T> {

    private final Class<T> typeParameterClass;

    /**
     * Constructor for StreamQueryResolver
     *
     * @param typeParameterClass class of the type parameter. Used in reflection operation to recursively fetch a property from an object.
     */
    public ReflectionBasedQueryResolver(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    /**
     * Method to query a stream by provided specification.
     * Converts the criterion into 'and' predicate.
     * Applies sorting. When sort field is not found returns empty stream.
     * Applies offset and limit on the query result.
     *
     * @param stream stream to be queried.
     * @param spec query specification.
     * @return stream result from queries.
     */
    @Override
    public Stream<T> query(Stream<T> stream, QuerySpec spec) {

        // filter
        Stream<Predicate<T>> predicateStream = spec.getFilterExpression().stream().map(this::toPredicate);
        var andPredicate = predicateStream.reduce(x -> true, Predicate::and);
        Stream<T> filteredStream  = stream.filter(andPredicate);

        // sort
        var sortField = spec.getSortField();

        if (sortField != null) {
            // if the sort field doesn't exist on the object -> return empty
            if (ReflectionUtil.getFieldRecursive(typeParameterClass, sortField) == null) {
                return Stream.empty();
            }
            var comparator = propertyComparator(spec.getSortOrder() == SortOrder.ASC, sortField);
            filteredStream = filteredStream.sorted(comparator);
        }

        // limit
        return filteredStream.skip(spec.getOffset()).limit(spec.getLimit());
    }

    private Predicate<T> toPredicate(Criterion criterion) {
        BaseCriterionToPredicateConverter<T> predicateConverter = new BaseCriterionToPredicateConverter<>() {
            @Override
            protected <R> R property(String key, Object object) {
                return ReflectionUtil.getFieldValueSilent(key, object);
            }
        };
        return predicateConverter.convert(criterion);
    }

}
