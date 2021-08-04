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

public class GetProcessGroupResponse {

    public Revision revision;
    public String id;
    public String uri;
    public Component component;
    public Status status;
    public int runningCount;
    public int stoppedCount;
    public int invalidCount;
    public int disabledCount;
    public int activeRemotePortCount;
    public int inactiveRemotePortCount;
    public int upToDateCount;
    public int locallyModifiedCount;
    public int staleCount;
    public int locallyModifiedAndStaleCount;
    public int syncFailureCount;
    public int localInputPortCount;
    public int localOutputPortCount;
    public int publicInputPortCount;
    public int publicOutputPortCount;
    public int inputPortCount;
    public int outputPortCount;

    public class Component {
        public String id;
        public String name;
        public String comments;
        public String flowfileConcurrency;
        public String flowfileOutboundPolicy;
        public int runningCount;
        public int stoppedCount;
        public int invalidCount;
        public int disabledCount;
        public int activeRemotePortCount;
        public int inactiveRemotePortCount;
        public int upToDateCount;
        public int locallyModifiedCount;
        public int staleCount;
        public int locallyModifiedAndStaleCount;
        public int syncFailureCount;
        public int localInputPortCount;
        public int localOutputPortCount;
        public int publicInputPortCount;
        public int publicOutputPortCount;
        public int inputPortCount;
        public int outputPortCount;
    }


    public class Status {
        public String id;
        public String name;
        public String statsLastRefreshed;
    }


}
