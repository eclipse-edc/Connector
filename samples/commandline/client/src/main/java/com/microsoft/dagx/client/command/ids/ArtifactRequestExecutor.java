/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.client.command.ids;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.TokenResult;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.dagx.client.command.http.HttpOperations.executePost;
import static com.microsoft.dagx.client.command.ids.IdsConstants.ID_URI;
import static com.microsoft.dagx.client.command.ids.IdsConstants.VERSION;

/**
 * Sends a {@link ArtifactRequestMessage} to an IDS Controller.
 */
public class ArtifactRequestExecutor implements CommandExecutor {

    @Override
    public CommandResult execute(ExecutionContext context) {

        List<String> params = context.getParams();
        if (params.size() < 2) {
            return new CommandResult(true, "Please specify an artifact id and connector id: ids request <artifact id> <connector id>.");
        }
        URI artifactId = URI.create(params.get(0));
        String connectorId = params.get(1);    // connector id is used as the scope of the token request

        // FIXME The IDS types require concrete classes. An issue needs to be filed with Fraunhofer to change them to interfaces
        ArrayList<URI> connectors = new ArrayList<>();
        connectors.add(ID_URI);

        IdentityService identityService = context.getService(IdentityService.class);

        TokenResult tokenResult = identityService.obtainClientCredentials(connectorId);

        if (!tokenResult.success()) {
            return new CommandResult(true, tokenResult.error());
        }

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getToken()).build();
        // TODO parameterize artifact urn

        ArtifactRequestMessage message = new ArtifactRequestMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(VERSION)
                ._securityToken_(token)
                ._issuerConnector_(ID_URI)
                ._senderAgent_(ID_URI)
                ._requestedArtifact_(artifactId)
                ._recipientConnector_(connectors)
                .build();
        return executePost("/api/ids/request", message, context);
    }
}
