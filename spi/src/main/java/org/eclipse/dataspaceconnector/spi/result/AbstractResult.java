package org.eclipse.dataspaceconnector.spi.result;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.List;

public abstract class AbstractResult<T, F extends Failure> {

    private final T content;
    private final F failure;

    protected AbstractResult(T content, F failure) {
        this.content = content;
        this.failure = failure;
    }

    public T getContent() {
        return content;
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean failed() {
        return !succeeded();
    }

    public String getFailure() {
        return failure.getMessages().stream().findFirst().orElseThrow(() -> new EdcException("This result is successful"));
    }

    public List<String> getFailures() {
        return failure.getMessages();
    }

    public F failure() {
        return failure;
    }
}
