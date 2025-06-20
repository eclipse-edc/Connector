/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-core"))

    api(project(":data-protocols:dsp:dsp-2024:dsp-http-api-configuration-2024"))
    api(project(":data-protocols:dsp:dsp-2024:dsp-http-dispatcher-2024"))
    api(project(":data-protocols:dsp:dsp-2024:dsp-catalog-2024"))
    api(project(":data-protocols:dsp:dsp-2024:dsp-negotiation-2024"))
    api(project(":data-protocols:dsp:dsp-2024:dsp-transfer-process-2024"))

    api(project(":data-protocols:dsp:dsp-version"))
}
