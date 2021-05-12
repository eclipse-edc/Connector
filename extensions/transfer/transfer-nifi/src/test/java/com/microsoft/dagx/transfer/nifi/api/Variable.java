/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.nifi.api;

public class Variable {
    private String name;
    private String value;
    private String processGroupId;

    public Variable() {
    }

    public Variable(String name, String value, String processGroupId) {
        this.name = name;
        this.value = value;
        this.processGroupId = processGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
    }
}
