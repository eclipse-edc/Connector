/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.s3.provision;

public class S3BucketProvisionerConfiguration {

    private final int maxRetries;
    private final int roleMaxSessionDuration;

    public S3BucketProvisionerConfiguration(int maxRetries, int roleMaxSessionDuration) {
        this.maxRetries = maxRetries;
        this.roleMaxSessionDuration = roleMaxSessionDuration;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRoleMaxSessionDuration() {
        return roleMaxSessionDuration;
    }

}
