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


import jakarta.json.JsonObject;

import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspTransferProcessService;
import org.eclipse.edc.spi.EdcException;

import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

public class DspTransferProcessServiceImpl implements DspTransferProcessService {

    private final TransferProcessService transferProcessService;

    private final JsonLdTransformerRegistry registry;

    public DspTransferProcessServiceImpl(TransferProcessService transferProcessService, JsonLdTransformerRegistry registry){
        this.transferProcessService = transferProcessService;
        this.registry = registry;
    }


    //Provider Side
    @Override
    public JsonObject getTransferProcessByID(String id) {

        var transferProcess = transferProcessService.findById(id);

        if (transferProcess == null){
            throw new ObjectNotFoundException(TransferProcess.class,id);
        }

        var result = registry.transform(transferProcess,JsonObject.class);

        if (result.failed()){
            throw new EdcException("Response could not be created");
        }

        return result.getContent(); //TODO Check if Return Value works correct
    }

    @Override
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        var dataRequest = registry.transform(jsonObject, DataRequest.class); //TODO Write transformer

        if (dataRequest.failed()){
            throw new InvalidRequestException("Request body was malformed");
        }

        var value = transferProcessService.initiateTransfer(dataRequest.getContent()); //TODO get TransferProcess as Return Value

        return null; //TODO RETURN Correct Value
    }

    @Override
    public void transferProcessStart(String id, JsonObject jsonObject) {
        //TODO START transferProcess
    }

    @Override
    public void transferProcessCompletion(String id, JsonObject jsonObject) {
        transferProcessService.complete(id); //TODO Write Correct Reason
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
