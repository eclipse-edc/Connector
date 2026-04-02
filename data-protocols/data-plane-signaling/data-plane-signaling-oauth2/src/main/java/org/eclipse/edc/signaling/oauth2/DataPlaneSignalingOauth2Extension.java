/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.signaling.oauth2.logic.Oauth2CredentialsSignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class DataPlaneSignalingOauth2Extension implements ServiceExtension {

    @Inject
    private SignalingAuthorizationRegistry signalingAuthorizationRegistry;
    @Inject
    private Oauth2Client oauth2Client;

    @Override
    public void initialize(ServiceExtensionContext context) {
        signalingAuthorizationRegistry.register(new Oauth2CredentialsSignalingAuthorization(oauth2Client));
    }
}
