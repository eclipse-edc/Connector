package com.microsoft.dagx.client.command.ids;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;

import java.net.URI;
import java.util.ArrayList;

import static com.microsoft.dagx.client.command.http.HttpOperations.executePost;
import static com.microsoft.dagx.client.command.ids.IdsConstants.ID_URI;
import static com.microsoft.dagx.client.command.ids.IdsConstants.VERSION;

/**
 * Sends a {@link ArtifactRequestMessage} to an IDS Controller.
 */
public class ArtifactRequestExecutor implements CommandExecutor {

    @Override
    public CommandResult execute(ExecutionContext context) {
        ArtifactRequestMessageBuilder messageBuilder = new ArtifactRequestMessageBuilder();

        // FIXME The IDS types require concrete classes. An issue needs to be filed with Fraunhofer to change them to interfaces
        ArrayList<URI> connectors = new ArrayList<>();
        connectors.add(ID_URI);

        // TODO add JWS ._securityToken_(jwsToken)
        // TODO parameterize artifact urn
        URI artifactUrn = URI.create("dagx:microsoft:artifacts:test");

        ArtifactRequestMessage message = new ArtifactRequestMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(VERSION)
                ._issuerConnector_(ID_URI)
                ._senderAgent_(ID_URI)
                ._requestedArtifact_(artifactUrn)
                ._recipientConnector_(connectors)
                .build();
        return executePost("/api/ids/request", message, context);
    }
}
