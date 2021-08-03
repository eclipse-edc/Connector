package org.eclipse.edc.transfer.demo.protocols.spi.stream.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.spi.types.domain.Polymorphic;

/**
 * The base message for the streaming demo pub/sub protocol.
 *
 * This class demonstrates the use of polymorphic JSON deserialization. Subclasses are registered with the type manager so that they canbe automatically deserialized.
 */
public abstract class PubSubMessage implements Polymorphic {
    public enum Protocol {
        SUBSCRIBE, UNSUBSCRIBE, CONNECT, DISCONNECT, PUBLISH, DATA
    }

    protected Protocol protocol;

    @JsonIgnore
    public Protocol getProtocol() {
        return protocol;
    }

    protected PubSubMessage(Protocol protocol) {
        this.protocol = protocol;
    }

}
