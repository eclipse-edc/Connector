/*
 *  Copyright (c) 2020, 2020-2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ion.model;

import java.util.Map;

public class DidOperation {
    private final Map<String, Object> content;
    private final DidOperationType type;
    private JwkKeyPair recovery;
    private JwkKeyPair update;
    private DidOperation previous;

    public DidOperation(DidOperationType type, Map<String, Object> content) {
        this.type = type;
        this.content = content;
    }

    public DidOperationType type() {
        return type;
    }

    public boolean isDeactivated() {
        return type() == DidOperationType.DEACTIVATE;
    }

    public JwkKeyPair getRecovery() {
        return recovery;
    }

    public void setRecovery(JwkKeyPair recovery) {
        this.recovery = recovery;
    }

    public JwkKeyPair getUpdate() {
        return update;
    }

    public void setUpdate(JwkKeyPair update) {
        this.update = update;
    }

    public DidOperation getPrevious() {
        return previous;
    }

    public void setPrevious(DidOperation previous) {
        this.previous = previous;
    }

    public Map<String, Object> getContent() {
        return content;
    }
}
