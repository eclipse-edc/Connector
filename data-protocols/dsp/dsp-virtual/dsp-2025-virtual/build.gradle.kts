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
    api(project(":data-protocols:dsp:dsp-core"))

    api(project(":data-protocols:dsp:dsp-2025:dsp-http-api-configuration-2025"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-http-dispatcher-2025"))
    api(project(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual:dsp-catalog-http-api-2025-virtual"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025"))
    api(project(":data-protocols:dsp:dsp-virtual:dsp-metadata-http-api-virtual"))
}
