/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.dataseed.nifi.api;

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
