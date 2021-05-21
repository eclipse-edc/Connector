package com.microsoft.dagx.transfer.demo.protocols.ws;

import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.DataMessage;

/**
 *
 */
@FunctionalInterface
public interface PubSubConsumer {

    void accept(DataMessage dataMessage);

    default void closed() {
    }

}
