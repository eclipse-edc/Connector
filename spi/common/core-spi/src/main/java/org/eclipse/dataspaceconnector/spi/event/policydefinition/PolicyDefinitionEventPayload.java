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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.dataspaceconnector.spi.event.policydefinition;

import org.eclipse.dataspaceconnector.spi.event.EventPayload;

public class PolicyDefinitionEventPayload extends EventPayload {

    protected String policyDefinitionId;

    public String getPolicyDefinitionId() {
        return policyDefinitionId;
    }

}
