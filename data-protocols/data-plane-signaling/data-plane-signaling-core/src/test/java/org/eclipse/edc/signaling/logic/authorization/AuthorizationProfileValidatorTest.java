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

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorizationProfileValidatorTest {

    private final SignalingAuthorizationRegistry registry = mock();
    private final SignalingAuthorization authorization = mock();
    private final AuthorizationProfileValidator validator = new AuthorizationProfileValidator(registry);

    @Test
    void shouldSucceed_whenNoAuthorizationProfile() {
        var result = validator.validate(instance(null));

        assertThat(result).isSucceeded();
        verifyNoInteractions(registry);
    }

    @Test
    void shouldFail_whenTypeNotRegistered() {
        var result = validator.validate(instance(new AuthorizationProfile("unknown", Map.of())));

        assertThat(result).isFailed();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        assertThat(result.getFailureDetail()).contains("'unknown'");
    }

    @Test
    void shouldFail_whenProfileInvalid() {
        var profile = new AuthorizationProfile("type", Map.of());
        when(registry.findByType("type")).thenReturn(authorization);
        when(authorization.validate(any(), any())).thenReturn(Result.failure("missing property"));

        var result = validator.validate(instance(profile));

        assertThat(result).isFailed();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        assertThat(result.getFailureDetail()).isEqualTo("missing property");
        verify(authorization).validate("dp-id", profile);
    }

    @Test
    void shouldSucceed_whenProfileValid() {
        when(registry.findByType("type")).thenReturn(authorization);
        when(authorization.validate(any(), any())).thenReturn(Result.success());

        var result = validator.validate(instance(new AuthorizationProfile("type", Map.of())));

        assertThat(result).isSucceeded();
    }

    private DataPlaneInstance instance(AuthorizationProfile profile) {
        return DataPlaneInstance.Builder.newInstance().id("dp-id").url("http://any").authorizationProfile(profile).build();
    }
}
