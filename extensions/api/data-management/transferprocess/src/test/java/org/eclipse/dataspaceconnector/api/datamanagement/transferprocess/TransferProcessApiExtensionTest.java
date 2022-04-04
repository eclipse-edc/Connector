/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform.TransferProcessTransformerTestData;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class TransferProcessApiExtensionTest {
    static Faker faker = new Faker();
    TransferProcessTransformerTestData data = new TransferProcessTransformerTestData();
    String contextAlias = faker.lorem().word();

    @Test
    void initialize(ServiceExtensionContext context, ObjectFactory factory) {
        var registry = new DtoTransformerRegistryImpl();
        context.registerService(DtoTransformerRegistry.class, registry);
        var webServiceMock = mock(WebService.class);
        context.registerService(WebService.class, webServiceMock);
        var mockConfiguration = new DataManagementApiConfiguration(contextAlias);
        context.registerService(DataManagementApiConfiguration.class, mockConfiguration);

        var extension = factory.constructInstance(TransferProcessApiExtension.class);
        extension.initialize(context);

        verify(webServiceMock).registerResource(eq(contextAlias), any(TransferProcessApiController.class));

        assertThat(registry.transform(data.entity.build(), TransferProcessDto.class).succeeded()).isTrue();
    }
}