/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.core.configuration.IllegalSettingException;
import org.eclipse.dataspaceconnector.ids.core.configuration.SettingResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class MultipartControllerSettingsFactoryTest {

    private URI connectorId;

    // mocks
    private SettingResolver settingResolver;

    @BeforeEach
    public void setup() throws URISyntaxException {
        connectorId = new URI("https://example.com");
        settingResolver = EasyMock.createMock(SettingResolver.class);
    }

    @Test
    public void testErrorsPartOfResponse() throws IllegalSettingException {
        // prepare
        var reason = "test-reason";
        var exception = new IllegalSettingException(reason);
        var multipartControllerSettingsFactory = new MultipartControllerSettingsFactory(settingResolver);

        EasyMock.expect(settingResolver.resolveId()).andThrow(exception);
        EasyMock.replay(settingResolver);

        // invoke
        var result = multipartControllerSettingsFactory.createRejectionMessageFactorySettings();

        //validate
        Assertions.assertEquals(1, result.getErrors().size());
        Assertions.assertEquals(reason, result.getErrors().get(0));
    }

    @Test
    public void testIdPartOfResponse() throws IllegalSettingException {
        // prepare
        var multipartControllerSettingsFactory = new MultipartControllerSettingsFactory(settingResolver);

        EasyMock.expect(settingResolver.resolveId()).andReturn(connectorId);
        EasyMock.replay(settingResolver);

        // invoke
        var result = multipartControllerSettingsFactory.createRejectionMessageFactorySettings();

        //validate
        Assertions.assertNotNull(result.getRejectionMessageFactorySettings());
        Assertions.assertEquals(connectorId, result.getRejectionMessageFactorySettings().getId());
    }
}
