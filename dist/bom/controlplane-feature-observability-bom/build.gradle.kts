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

// metrics (Micrometer) and OpenTelemetry logging, for the single and the multi-participant control planes
dependencies {
    api(project(":extensions:common:metrics:micrometer-core"))
    api(project(":extensions:common:http:jetty-micrometer"))
    api(project(":extensions:common:http:jersey-micrometer"))
    api(project(":extensions:common:otel-monitor"))
}

edcBuild {

}
