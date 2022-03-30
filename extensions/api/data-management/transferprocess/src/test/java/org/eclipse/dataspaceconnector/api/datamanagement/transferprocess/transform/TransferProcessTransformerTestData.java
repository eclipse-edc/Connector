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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import static org.mockito.Mockito.mock;

public class TransferProcessTransformerTestData {
    static Faker faker = new Faker();

    DtoTransformerRegistry registry = mock(DtoTransformerRegistry.class);
    TransformerContext context = new DtoTransformerRegistryImpl.DtoTransformerContext(registry);
    String id = faker.lorem().word();
    TransferProcess.Type type = faker.options().option(TransferProcess.Type.class);
    TransferProcessStates state = faker.options().option(TransferProcessStates.class);
    String errorDetail = faker.lorem().word();

    DataRequest dataRequest = DataRequest.Builder.newInstance()
            .dataDestination(DataAddress.Builder.newInstance().type(faker.lorem().word()).build())
            .build();
    DataRequestDto dataRequestDto = DataRequestDto.Builder.newInstance().build();

    public TransferProcess.Builder entity = TransferProcess.Builder.newInstance()
            .id(id)
            .type(type)
            .state(state.code())
            .errorDetail(errorDetail)
            .dataRequest(dataRequest);

    TransferProcessDto.Builder dto = TransferProcessDto.Builder.newInstance()
            .id(id)
            .type(type.name())
            .state(state.name())
            .errorDetail(errorDetail)
            .dataRequest(dataRequestDto);
}