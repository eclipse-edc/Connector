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

package org.eclipse.edc.azure.cosmos.dialect;

import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is an implementation of {@link ConditionExpression} which rewrite the path expression {@code obj.something = foo}
 * with an EXISTS expression if a collection field is present in the dot notation path. This is a rewrite rule for
 * supporting array querying in CosmosDB
 * <p>
 * For example the expression {@code obj.collections.name = foo }
 * will be rewritten as {@code EXITS(SELECT VALUE t from t in obj.collections WHERE t.name = @obj.collections.name )}
 * if the field collections is an actual Java collection detected in the {@link CosmosExistsExpression#parse } method
 */
public class CosmosExistsExpression extends ConditionExpression {

    private static final String SUBQUERY_TARGET_ALIAS = "t";
    private static final String EXISTS_SUBQUERY = " EXISTS(SELECT VALUE %s FROM %s IN %s WHERE %s %s %s)";
    private final Class<?> target;
    private final String objectPrefix;
    private final String collectionPrefix;

    private CosmosExistsExpression(Class<?> target, Criterion criterion, String collectionPrefix, String objectPrefix) {
        super(criterion);
        Objects.requireNonNull(collectionPrefix, "Collection prefix should not be null in case of exists expression");
        this.target = target;
        this.objectPrefix = objectPrefix;
        this.collectionPrefix = collectionPrefix;
    }


    /**
     * Construct the {@link CosmosExistsExpression} if the Exists expression can be applied to the input {@link Criterion} by checking if in the left operand dot notation,
     * a field of type {@link Collection} is present in the path.
     *
     * @param clazz        the class used for checking the field collection
     * @param criterion    the filter criteria
     * @param objectPrefix the object prefix
     * @return Optionally if the collection field is detected returns a {@link CosmosExistsExpression}
     */
    public static CosmosExistsExpression parse(Class<?> clazz, Criterion criterion, String objectPrefix) {
        var path = criterion.getOperandLeft().toString();
        var fields = ReflectionUtil.getAllFieldsRecursiveWithPath(clazz, path);
        var collectionIndexes = collectionIndex(fields);
        if (collectionIndexes.size() != 1) {
            return null;
        } else {
            var collectionIdx = collectionIndexes.get(0);
            // if the collection field is the last the exists expression cannot be used
            if (collectionIdx == fields.size() - 1) {
                return null;
            }
            var pathParts = Arrays.asList(path.split(Pattern.quote(".")));

            // Partition the path in 2 groups based on the collection field index
            // E.g. [obj, collections, name] => [obj, collections] - [name]
            var pathPartitions = pathParts.stream().collect(Collectors.partitioningBy((item) -> pathParts.indexOf(item) <= collectionIdx));
            var localPrefix = String.join(".", pathPartitions.get(true));
            var localField = String.join(".", pathPartitions.get(false));
            var newCriterion = new Criterion(localField, criterion.getOperator(), criterion.getOperandRight());

            return new CosmosExistsExpression(clazz, newCriterion, localPrefix, objectPrefix);
        }

    }

    private static List<Integer> collectionIndex(List<Field> fields) {
        return IntStream.range(0, fields.size())
                .filter(idx -> Collection.class.isAssignableFrom(fields.get(idx).getType()))
                .boxed()
                .collect(Collectors.toList());
    }


    @Override
    public String getFieldPath() {
        return collectionPrefix + "." + getCriterion().getOperandLeft().toString();
    }

    @Override
    public String toExpressionString() {
        var operandLeft = SUBQUERY_TARGET_ALIAS + "." + CosmosDocument.sanitize(getCriterion().getOperandLeft().toString());
        var collectionField = objectPrefix != null ? objectPrefix + "." + collectionPrefix : collectionPrefix;
        return String.format(EXISTS_SUBQUERY, SUBQUERY_TARGET_ALIAS, SUBQUERY_TARGET_ALIAS, quotePath(collectionField), quotePath(operandLeft), getCriterion().getOperator(), toValuePlaceholder());
    }
}
