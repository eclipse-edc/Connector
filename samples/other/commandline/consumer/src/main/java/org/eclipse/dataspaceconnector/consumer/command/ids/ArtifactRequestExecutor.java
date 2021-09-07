/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.consumer.command.ids;

import de.fraunhofer.iais.eis.*;
import org.eclipse.dataspaceconnector.consumer.command.CommandExecutor;
import org.eclipse.dataspaceconnector.consumer.command.CommandResult;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.dataspaceconnector.consumer.command.http.HttpOperations.executePost;
import static org.eclipse.dataspaceconnector.consumer.command.ids.IdsConstants.ID_URI;
import static org.eclipse.dataspaceconnector.consumer.command.ids.IdsConstants.VERSION;

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
