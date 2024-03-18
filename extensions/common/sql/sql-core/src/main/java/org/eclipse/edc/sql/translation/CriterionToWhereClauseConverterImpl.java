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

import org.eclipse.edc.spi.query.Criterion;

public class CriterionToWhereClauseConverterImpl implements CriterionToWhereClauseConverter {

    private final TranslationMapping translationMapping;
    private final SqlOperatorTranslator operatorTranslator;

    public CriterionToWhereClauseConverterImpl(TranslationMapping translationMapping, SqlOperatorTranslator operatorTranslator) {
        this.translationMapping = translationMapping;
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public WhereClause convert(Criterion criterion) {
        var operator = operatorTranslator.translate(criterion.getOperator().toLowerCase());
        if (operator == null) {
            throw new IllegalArgumentException("The operator '%s' is not supported".formatted(criterion.getOperator()));
        }

        if (!operator.rightOperandClass().isAssignableFrom(criterion.getOperandRight().getClass())) {
            throw new IllegalArgumentException("The operator '%s' requires the right-hand operand to be of type %s"
                    .formatted(criterion.getOperator(), operator.rightOperandClass().getSimpleName()));
        }

        var whereClause = translationMapping.getWhereClause(criterion, operator);
        if (whereClause == null) {
            return new WhereClause("0 = ?", 1);
        }

        return whereClause;
    }

}
