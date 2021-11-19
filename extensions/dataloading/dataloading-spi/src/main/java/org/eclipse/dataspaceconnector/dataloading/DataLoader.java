package org.eclipse.dataspaceconnector.dataloading;


import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataLoader<T> {
    private Collection<Function<T, ValidationResult>> validationPredicates;
    private DataSink<T> sink;

    protected DataLoader() {
        validationPredicates = new ArrayList<>();
    }

    public void insert(T item) {
        // see that the item satisfies all predicates
        var failedValidations = validate(item).filter(vr -> !vr.isValid())
                .collect(Collectors.toUnmodifiableList());

        // throw exception if item does not pass all validations
        if (!failedValidations.isEmpty()) {
            throw new ValidationException(failedValidations.stream().map(ValidationResult::getError).collect(Collectors.joining("; ")));
        }

        sink.accept(item);
    }

    public void insertAll(Collection<T> items) {

        var allValidationResults = items.stream().flatMap(this::validate);

        var errorMessages = allValidationResults.filter(ValidationResult::isInvalid).map(ValidationResult::getError).collect(Collectors.toList());

        if (!errorMessages.isEmpty()) {
            throw new ValidationException(String.join("; ", errorMessages));
        }

        items.forEach(sink::accept);
    }

    private Stream<ValidationResult> validate(T item) {
        return validationPredicates.stream().map(vr -> vr.apply(item));
    }

    public static final class Builder<T> {
        private final DataLoader<T> loader;

        private Builder() {
            loader = new DataLoader<>();
        }

        public static <T> Builder<T> newInstance() {
            return new Builder<>();
        }

        public Builder<T> sink(DataSink<T> sink) {
            loader.sink = sink;
            return this;
        }

        public Builder<T> andPredicate(Function<T, ValidationResult> predicate) {
            loader.validationPredicates.add(predicate);
            return this;
        }

        public Builder<T> predicates(Collection<Function<T, ValidationResult>> predicates) {
            loader.validationPredicates = predicates;
            return this;
        }

        public DataLoader<T> build() {
            return loader;
        }
    }
}


