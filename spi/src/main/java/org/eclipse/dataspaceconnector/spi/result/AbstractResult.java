package org.eclipse.dataspaceconnector.spi.result;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public abstract class AbstractResult<T, F extends Failure> {

    private final T content;
    private final F failure;

    protected AbstractResult(T content, F failure) {
        this.content = content;
        this.failure = failure;
    }

    @NotNull
    public T getContent() {
        Objects.requireNonNull(content);
        return content;
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean failed() {
        return !succeeded();
    }

    public String getFailure() {
        Objects.requireNonNull(failure);
        return failure.getMessages().stream().findFirst().orElseThrow(() -> new EdcException("This result is successful"));
    }

    public List<String> getFailures() {
        Objects.requireNonNull(failure);
        return failure.getMessages();
    }

    public F failure() {
        return failure;
    }
}
