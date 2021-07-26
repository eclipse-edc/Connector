/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.dataseed.nifi.api;

import java.util.List;

public class ControllerServiceResponse {
    public String currentTime;
    public List<ControllerService> controllerServices;
}
