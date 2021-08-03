/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.dataseed.nifi.api;

import java.util.List;

public class ControllerServiceResponse {
    public String currentTime;
    public List<ControllerService> controllerServices;
}
