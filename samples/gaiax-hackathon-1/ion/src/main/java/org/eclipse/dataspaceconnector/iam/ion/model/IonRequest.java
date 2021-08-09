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

package org.eclipse.dataspaceconnector.iam.ion.model;

public abstract class IonRequest {

    private final String type;

    protected IonRequest(String type) {
        this.type = type;
    }

    public abstract SuffixData getSuffixData();

    public abstract Delta getDelta();

    public String getType() {
        return type;
    }
}
