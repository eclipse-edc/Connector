package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public interface ValidationRule<T> extends Function<T, Result<Void>> {

    static <T> ValidationRule<T> identity() {
        return t -> Result.success();
    }

    default ValidationRule<T> and(ValidationRule<T> other) {
        return t -> {
            Result<Void> thisResult = this.apply(t);
            Result<Void> otherResult = other.apply(t);

            var thisFailureMessages = thisResult.failed() ? thisResult.getFailureMessages().stream() : Stream.<String>empty();
            var otherFailureMessages = otherResult.failed() ? otherResult.getFailureMessages().stream() : Stream.<String>empty();
            var totalFailureMessages = Stream.concat(thisFailureMessages, otherFailureMessages).collect(Collectors.toList());
            if (totalFailureMessages.isEmpty()) {
                return Result.success();
            } else {
                return Result.failure(totalFailureMessages);
            }
        };
    }
}
