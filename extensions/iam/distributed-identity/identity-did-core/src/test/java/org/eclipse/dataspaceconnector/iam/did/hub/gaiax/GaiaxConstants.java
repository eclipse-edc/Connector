/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.gaiax;

/**
 *
 */
public interface GaiaxConstants {

    @Deprecated
    String CONSUMER_WRITE_COMMIT_TEMP_URL = "http://localhost:9191/api/identity-hub/collections-commit";

    String CONSUMER_WRITE_COMMIT_URL = "http://localhost:9191/api/identity-hub/collections";

    String PRODUCER_WRITE_COMMIT_URL = "http://localhost:8181/api/identity-hub/collections";

    String PRODUCER_OBJECT_QUERY_URL = "http://localhost:8181/api/identity-hub/query-objects";

    String CONSUMER_OBJECT_QUERY_URL = "http://localhost:8181/api/identity-hub/query-objects";

    String PRODUCER_COMMIT_QUERY_URL = "http://localhost:8181/api/identity-hub/query-commits";

    String CONSUMER_COMMIT_QUERY_URL = "http://localhost:8181/api/identity-hub/query-commits";

}
