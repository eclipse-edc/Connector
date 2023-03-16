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
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.TransferProcessService;

public class TransferProcessServiceImpl implements TransferProcessService {

    private final TransferProcessManager transferProcessManager;

    public TransferProcessServiceImpl(TransferProcessManager transferProcessManager){
        this.transferProcessManager = transferProcessManager;
    }


    //Provider Side
    @Override
    public JsonObject getTransferProcessByID(String id) {

        return null;
    }

    @Override
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        return null;
    }

    @Override
    public void transferProcessStart(String id, JsonObject jsonObject) {
    }

    @Override
    public void transferProcessCompletion(String id, JsonObject jsonObject) {

    }

    @Override
    public void transferProcessTermination(String id, JsonObject jsonObject) {
        jsonObject.getString("@id");
        var cmd = new TerminateTransferCommand(id,"API-CALL"); //TODO Generate better Reason
        transferProcessManager.enqueueCommand(cmd);
    }

    @Override
    public void transferProcessSuspension(String id, JsonObject jsonObject) {
        var dataRequest = DataRequest.Builder.newInstance().build();
    }
}
