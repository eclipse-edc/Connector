package org.eclipse.dataspaceconnector.spi.result;

import java.util.List;

class GenericFailure implements Failure {

    private List<String> messages;

    public GenericFailure(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }
}
