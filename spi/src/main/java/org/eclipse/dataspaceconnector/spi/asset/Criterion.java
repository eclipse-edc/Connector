package org.eclipse.dataspaceconnector.spi.asset;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 * "operandLeft" = "name",
 * "operator" = "IN",
 * "operandRight" = "(max, moritz, denis, dominik)"
 * <p>
 * translates to select * where name = somename
 * }
 */
public class Criterion {
    @JsonProperty("left")
    private String operandLeft;
    @JsonProperty("op")
    private String operator;
    @JsonProperty("right")
    private String operandRight;

    private Criterion() {
        //for json
    }

    public Criterion(String left, String op, String right) {
        this.operandLeft = left;
        this.operator = op;
        this.operandRight = right;
    }

    public String getOperandLeft() {
        return operandLeft;
    }

    public String getOperator() {
        return operator;
    }

    public String getOperandRight() {
        return operandRight;
    }
}
