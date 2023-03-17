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

package org.eclipse.edc.protocol.dsp.spi.controlplane.service;

import jakarta.json.JsonObject;

public interface DspTransferProcessService {

    JsonObject getTransferProcessByID(String id);

    JsonObject initiateTransferProcess(JsonObject jsonObject);

    void transferProcessStart(String id, JsonObject jsonObject);

    void transferProcessCompletion(String id, JsonObject jsonObject);

    void transferProcessTermination(String id, JsonObject jsonObject);

    void transferProcessSuspension(String id, JsonObject jsonObject);

}
