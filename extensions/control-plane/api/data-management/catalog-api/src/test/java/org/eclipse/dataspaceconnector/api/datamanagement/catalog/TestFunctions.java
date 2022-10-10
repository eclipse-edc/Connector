package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import org.eclipse.dataspaceconnector.api.model.CriterionDto;

public class TestFunctions {
    public static CriterionDto createCriterionDto(String left, String op, Object right) {
        return CriterionDto.Builder.newInstance().operandLeft(left).operator(op).operandRight(right).build();
    }

}
