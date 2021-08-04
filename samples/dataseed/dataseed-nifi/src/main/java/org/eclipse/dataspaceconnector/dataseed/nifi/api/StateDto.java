/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataseed.nifi.api;

public class StateDto {
    public String id;
    public String state;
    public boolean disconnectedNodeAcknowledged;

    public StateDto(String processGroup, String state, boolean disconnectedNodeAck) {
        id = processGroup;
        this.state = state;
        disconnectedNodeAcknowledged = disconnectedNodeAck;
    }

    public StateDto() {
    }
}
