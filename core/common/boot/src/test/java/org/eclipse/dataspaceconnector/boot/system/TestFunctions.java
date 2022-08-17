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

package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.boot.system.testextensions.BaseExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.CoreExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.DefaultProviderExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.DependentExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.ProviderExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;

import java.util.ArrayList;
import java.util.List;

public class TestFunctions {
    public static List<ServiceExtension> createList(ServiceExtension... extensions) {
        var l = new ArrayList<>(List.of(extensions));
        l.add(new CoreExtension());
        l.add(new BaseExtension());
        return l;
    }

    public static List<InjectionContainer<ServiceExtension>> createInjectionContainers(List<ServiceExtension> extensions) {
        var dg = new DependencyGraph();
        return dg.of(extensions);

    }

    public static ServiceExtension createProviderExtension(boolean isDefault) {

        return isDefault ? new DefaultProviderExtension() : new ProviderExtension();
    }

    public static ServiceExtension createDependentExtension(boolean isRequired) {

        return isRequired ? new RequiredDependentExtension() : new DependentExtension();
    }
}
