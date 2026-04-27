/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:control-plane:store:sql:participantcontext-store-sql"))
    api(project(":extensions:control-plane:store:sql:participantcontext-config-store-sql"))
    api(project(":extensions:common:store:sql:cel-store-sql"))
    api(project(":extensions:common:store:sql:task-store-sql"))
}

edcBuild {

}
