/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataseed.nifi.api;

public class ControllerService {
    public String id;
    public String uri;
    public Component component;
    public Revision revision;

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public Component getComponent() {
        return component;
    }
}
