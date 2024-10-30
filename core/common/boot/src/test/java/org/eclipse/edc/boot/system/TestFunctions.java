/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.system.testextensions.BaseExtension;
import org.eclipse.edc.boot.system.testextensions.CoreExtension;
import org.eclipse.edc.boot.system.testextensions.DependentExtension;
import org.eclipse.edc.boot.system.testextensions.ProviderDefaultServicesExtension;
import org.eclipse.edc.boot.system.testextensions.ProviderExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.ArrayList;
import java.util.List;

public class TestFunctions {
    public static List<ServiceExtension> createList(ServiceExtension... extensions) {
        var l = new ArrayList<>(List.of(extensions));
        l.add(new CoreExtension());
        l.add(new BaseExtension());
        return l;
    }

    public static ServiceExtension createProviderExtension(boolean isDefault) {

        return isDefault ? new ProviderDefaultServicesExtension() : new ProviderExtension();
    }

    public static ServiceExtension createDependentExtension(boolean isRequired) {

        return isRequired ? new RequiredDependentExtension() : new DependentExtension();
    }
}
