/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.nifi.api;

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
