/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - refactor DSP module structure to make versions pluggable
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-core:dsp-http-api-base-configuration"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-http-api-configuration-2025"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-catalog-2025"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-transfer-process-2025"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-negotiation-2025"))
}
