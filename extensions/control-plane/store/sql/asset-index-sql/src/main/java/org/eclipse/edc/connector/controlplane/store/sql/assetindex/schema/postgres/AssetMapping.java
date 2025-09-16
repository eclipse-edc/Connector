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

package org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.SqlOperator;
import org.eclipse.edc.sql.translation.TranslationMapping;
import org.eclipse.edc.sql.translation.WhereClause;

import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/**
 * Maps fields of a {@link Asset} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class AssetMapping extends TranslationMapping {

    public AssetMapping(AssetStatements statements) {
        add("id", statements.getAssetIdColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("properties", new JsonFieldTranslator(statements.getPropertiesColumn()));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add("dataAddress", new JsonFieldTranslator(statements.getDataAddressColumn()));
        add("participantContextId", statements.getParticipantContextIdColumn());
    }

    /**
     * Permit to get the field translator for properties when only the property name is defined.
     * It tries to get it with the argument passed, if null is returned it looks up into 'properties', if null is returned
     * it looks into properties wrapping the left operand with '', to permit handling property keys that contain a dot.
     *
     * @param fieldPath the path name.
     * @return a function that translates the right operand class into the left operand.
     */
    @Override
    public Function<Class<?>, String> getFieldTranslator(String fieldPath) {
        return requireNonNullElse(super.getFieldTranslator(fieldPath), fieldPath.contains("'")
                ? super.getFieldTranslator("properties.%s".formatted(fieldPath))
                : super.getFieldTranslator("properties.'%s'".formatted(fieldPath)));
    }

    /**
     * Permit to get the {@link WhereClause} for properties when only the property name is defined.
     * It tries to get it with the argument passed, if null is returned it looks up into 'properties', if null is returned
     * it looks into properties wrapping the left operand with '', to permit handling property keys that contain a dot.
     *
     * @param criterion the criterion.
     * @param operator  the operator.
     * @return the {@link WhereClause}.
     */
    @Override
    public WhereClause getWhereClause(Criterion criterion, SqlOperator operator) {
        return requireNonNullElse(super.getWhereClause(criterion, operator), criterion.getOperandLeft().toString().contains("'")
                ? super.getWhereClause(criterion.withLeftOperand("properties.%s".formatted(criterion.getOperandLeft())), operator)
                : super.getWhereClause(criterion.withLeftOperand("properties.'%s'".formatted(criterion.getOperandLeft())), operator));
    }
}
