package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;

public class CompositeValidationRule<T> implements ValidationRule<T> {

    private final List<ValidationRule<T>> rules;

    public CompositeValidationRule(List<ValidationRule<T>> rules) {
        this.rules = rules;
    }

    @Override
    public Result<Void> apply(T object) {
        return rules.stream().reduce(t -> Result.success(), ValidationRule::and).apply(object);
    }
}
