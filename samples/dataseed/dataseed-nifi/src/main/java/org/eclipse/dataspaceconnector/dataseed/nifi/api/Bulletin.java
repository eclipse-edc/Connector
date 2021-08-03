/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.dataseed.nifi.api;

public class Bulletin {
    public int id;
    public String groupId;
    public String sourceId;
    public String timestamp;
    public boolean canRead;
    public BulletinDetails bulletin;
}
