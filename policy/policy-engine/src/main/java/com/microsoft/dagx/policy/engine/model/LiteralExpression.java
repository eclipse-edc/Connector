package com.microsoft.dagx.policy.engine.model;

/**
 * A literal value used as an expression.
 */
public class LiteralExpression extends Expression {
    private String value;

    public LiteralExpression(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitLiteralExpression(this);
    }


}
