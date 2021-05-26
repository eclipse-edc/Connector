package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.transfer.demo.protocols.common.ProtocolsSecretToken;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamContext;
import com.microsoft.dagx.transfer.demo.protocols.ws.WsPushStreamSession;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

import static java.lang.String.format;

/**
 *
 */
public class PushStreamContext implements StreamContext {
    private Vault vault;
    private ObjectMapper objectMapper;
    private Monitor monitor;

    public PushStreamContext(Vault vault, ObjectMapper objectMapper, Monitor monitor) {
        this.vault = vault;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public StreamSession createSession(String uri, String destinationName, String secretName) {
        var accessToken = vault.resolveSecret(secretName);
        var destinationToken = readAccessToken(accessToken, secretName);
        var endpointUri = URI.create(uri);
        if ("ws".equalsIgnoreCase(endpointUri.getScheme())) {
            return new WsPushStreamSession(endpointUri, destinationName, destinationToken, objectMapper, monitor);
        } else {
            throw new UnsupportedOperationException("Unsupported scheme: " + endpointUri.getScheme());
        }
    }

    private String readAccessToken(@Nullable String accessToken, String destinationSecretName) {
        if (accessToken == null) {
            throw new DagxException("Access token not found in vault: " + destinationSecretName);
        }
        try {
            return objectMapper.readValue(accessToken, ProtocolsSecretToken.class).getToken();
        } catch (Exception e) {
            // NB: do not log the exception to avoid leaking credential information
            throw new DagxException(format("Error deserializing access token: %s [%s]", destinationSecretName, e.getClass().getName()));
        }
    }

}
