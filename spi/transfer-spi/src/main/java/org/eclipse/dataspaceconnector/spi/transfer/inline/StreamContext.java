package org.eclipse.dataspaceconnector.spi.transfer.inline;

/**
 * A context for creating stream sessions.
 */
public interface StreamContext {

    /**
     * Creates a stream session.
     *
     * @param uri        the stream endpoint uri
     * @param topicName  the topic name data is to be sent to
     * @param secretName the topic secret to be resolved by the context
     * @return the session
     */
    StreamSession createSession(String uri, String topicName, String secretName);

}
