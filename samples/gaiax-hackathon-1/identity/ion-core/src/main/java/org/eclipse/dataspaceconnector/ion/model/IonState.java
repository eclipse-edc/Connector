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

package org.eclipse.dataspaceconnector.ion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IonState {
    private final SuffixData suffixData;
    private final Delta delta;

    @JsonCreator
    public IonState(@JsonProperty("suffixData") SuffixData suffixData,
                    @JsonProperty("delta") Delta delta) {
        this.suffixData = suffixData;
        this.delta = delta;
    }

    public SuffixData getSuffixData() {
        return suffixData;
    }

    public Delta getDelta() {
        return delta;
    }
}
