/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.List;

@OpenAPIDefinition
public interface TransferProcessApi {

    List<TransferProcessDto> getAllTransferProcesses(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField);

    TransferProcessDto getTransferProcess(String id);

    String getTransferProcessState(String id);

    void cancelTransferProcess(String id);

    void deprovisionTransferProcess(String id);

    String initiateTransfer(String assetId, TransferRequestDto transferRequest);
}
