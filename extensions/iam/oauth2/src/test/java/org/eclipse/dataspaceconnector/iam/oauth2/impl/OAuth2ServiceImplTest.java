/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2ServiceImplTest {
    private OAuth2ServiceImpl authService;
    private DecodedJWT jwt;

    @Test
    public void verifyNoAudienceToken() {
        EasyMock.expect(jwt.getAudience()).andReturn(null);
        EasyMock.expect(jwt.getExpiresAt()).andReturn(new Date(System.currentTimeMillis() + 10000000)).anyTimes();
        EasyMock.expect(jwt.getNotBefore()).andReturn(new Date(System.currentTimeMillis() - 10000000)).anyTimes();
        EasyMock.replay(jwt);
        var result = authService.validateToken(jwt, "test.audience");
        assertFalse(result.valid());
        EasyMock.verify(jwt);
    }

    @Test
    public void verifyInvalidAudienceToken() {
        EasyMock.expect(jwt.getAudience()).andReturn(List.of("different.audience")).atLeastOnce();
        EasyMock.expect(jwt.getExpiresAt()).andReturn(new Date(System.currentTimeMillis() + 10000000)).anyTimes();
        EasyMock.expect(jwt.getNotBefore()).andReturn(new Date(System.currentTimeMillis() - 10000000)).anyTimes();
        EasyMock.replay(jwt);
        var result = authService.validateToken(jwt, "test.audience");
        assertFalse(result.valid());
        EasyMock.verify(jwt);
    }

    @Test
    public void verifyInvalidAttemptUseNotBeforeToken() {
        EasyMock.expect(jwt.getAudience()).andReturn(List.of("test.audience")).atLeastOnce();
        EasyMock.expect(jwt.getExpiresAt()).andReturn(new Date(System.currentTimeMillis() + 10000000)).anyTimes();
        EasyMock.expect(jwt.getNotBefore()).andReturn(new Date(System.currentTimeMillis() + 10000000)).atLeastOnce();
        EasyMock.replay(jwt);
        var result = authService.validateToken(jwt, "test.audience");
        assertFalse((result.valid()));
        EasyMock.verify(jwt);
    }

    @Test
    public void verifyExpiredToken() {
        EasyMock.expect(jwt.getAudience()).andReturn(List.of("test.audience")).atLeastOnce();
        EasyMock.expect(jwt.getExpiresAt()).andReturn(new Date(System.currentTimeMillis() - 10000000)).atLeastOnce();
        EasyMock.expect(jwt.getNotBefore()).andReturn(new Date(System.currentTimeMillis() - 10000000)).anyTimes();
        EasyMock.replay(jwt);
        var result = authService.validateToken(jwt, "test.audience");
        assertFalse((result.valid()));
        EasyMock.verify(jwt);
    }

    @Test
    public void verifyValidJwt() {
        EasyMock.expect(jwt.getAudience()).andReturn(List.of("test.audience")).atLeastOnce();
        EasyMock.expect(jwt.getExpiresAt()).andReturn(new Date(System.currentTimeMillis() + 10000000)).atLeastOnce();
        EasyMock.expect(jwt.getNotBefore()).andReturn(new Date(System.currentTimeMillis() - 10000000)).atLeastOnce();
        EasyMock.expect(jwt.getClaims()).andReturn(Collections.emptyMap()).atLeastOnce();
        EasyMock.replay(jwt);
        var result = authService.validateToken(jwt, "test.audience");
        assertTrue(result.valid());
        EasyMock.verify(jwt);
    }

    @BeforeEach
    void setUp() {
        jwt = EasyMock.createMock(DecodedJWT.class);
        authService = new OAuth2ServiceImpl(OAuth2Configuration.Builder.newInstance().build());
    }
}
