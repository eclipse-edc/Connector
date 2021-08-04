package org.eclipse.dataspaceconnector.transfer.demo.protocols.ws;

import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.DataMessage;

/**
 *
 */
@FunctionalInterface
public interface PubSubConsumer {

    void accept(DataMessage dataMessage);

    default void closed() {
    }

}
