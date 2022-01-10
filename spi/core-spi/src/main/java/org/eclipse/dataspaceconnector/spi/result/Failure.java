package org.eclipse.dataspaceconnector.spi.result;

import java.util.List;

public class Failure {
    private final List<String> messages;

    public Failure(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
