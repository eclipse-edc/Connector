/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.logic.authorization;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Validates the authorization profile of a Data Plane instance before it is registered, with the
 * {@link org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization} registered for the profile type.
 */
public class AuthorizationProfileValidator {

    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry;

    public AuthorizationProfileValidator(SignalingAuthorizationRegistry signalingAuthorizationRegistry) {
        this.signalingAuthorizationRegistry = signalingAuthorizationRegistry;
    }

    /**
     * Validates the authorization profile of the given instance. An instance without a profile is valid.
     *
     * @param instance the Data Plane instance
     * @return success if the profile can be used, a bad request otherwise
     */
    public ServiceResult<Void> validate(DataPlaneInstance instance) {
        var profile = instance.getAuthorizationProfile();
        if (profile == null) {
            return ServiceResult.success();
        }

        var authorization = signalingAuthorizationRegistry.findByType(profile.type());
        if (authorization == null) {
            return ServiceResult.badRequest("No signaling authorization is registered for the authorization profile type '%s'"
                    .formatted(profile.type()));
        }

        var result = authorization.validate(instance.getId(), profile);
        return result.succeeded() ? ServiceResult.success() : ServiceResult.badRequest(result.getFailureMessages());
    }
}
