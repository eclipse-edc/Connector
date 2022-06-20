/*
 *  Copyright (c) 2021 Microsoft Corporation
 *  Copyright (c) 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Siemens AG - changes to make it compatible with AWS S3 presigned URL for upload
 *
 */

package org.eclipse.dataspaceconnector.dataplane.cloud.http.pipeline;

public final class PresignedHttpDataAddressSchema {
    /**
     * The HTTP transfer type.
     */
    public static final String TYPE = "PresignedHttpData";

    private PresignedHttpDataAddressSchema() {}
}
