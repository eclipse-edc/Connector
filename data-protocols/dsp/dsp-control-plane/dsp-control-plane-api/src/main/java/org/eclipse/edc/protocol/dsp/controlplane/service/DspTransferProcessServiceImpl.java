/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspTransferProcessService;
import org.eclipse.edc.spi.types.TypeManager;

public class DspTransferProcessServiceImpl implements DspTransferProcessService {

    private final TransferProcessService transferProcessService;

    private final ObjectMapper mapper;

    public DspTransferProcessServiceImpl(TransferProcessService transferProcessService, TypeManager typeManager){
        this.transferProcessService = transferProcessService;
        this.mapper = typeManager.getMapper("json-ld");
    }


    //Provider Side
    @Override
    public JsonObject getTransferProcessByID(String id) {

        var transferProcess = transferProcessService.findById(id);

        return mapper.convertValue(transferProcess,JsonObject.class); //TODO Check if Return Value works correct
    }

    @Override
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        var dataRequest = DataRequest.Builder.newInstance().build();

        //TODO Build DataRequest

        var value = transferProcessService.initiateTransfer(dataRequest);

        return null; //TODO RETURN Correct Value
    }

    @Override
    public void transferProcessStart(String id, JsonObject jsonObject) {

        //TODO START transferProcess
    }

    @Override
    public void transferProcessCompletion(String id, JsonObject jsonObject) {
        transferProcessService.terminate(id, "API Call"); //TODO Write Correct Reason
    }

    @Override
    public void transferProcessTermination(String id, JsonObject jsonObject) {
        transferProcessService.terminate(id,"API CALL"); //TODO Write Correct Reason
    }

    @Override
    public void transferProcessSuspension(String id, JsonObject jsonObject) {
        //TODO SUSPEND transferProcess
    }
}
