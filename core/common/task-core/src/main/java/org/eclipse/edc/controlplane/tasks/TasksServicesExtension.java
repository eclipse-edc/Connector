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

package org.eclipse.edc.controlplane.tasks;

import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.controlplane.tasks.TaskTypes.TYPES;
import static org.eclipse.edc.controlplane.tasks.TasksServicesExtension.NAME;

@Extension(NAME)
public class TasksServicesExtension implements ServiceExtension {

    public static final String NAME = "Tasks Core Services";

    @Inject
    private TaskStore taskStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typeManager;

    private TaskObservable observable;


    @Override
    public void initialize(ServiceExtensionContext context) {
        TYPES.forEach(typeManager::registerTypes);
    }

    @Provider
    public TaskService taskService() {
        return new TaskServiceImpl(taskStore, taskObservable(), transactionContext);
    }

    @Provider
    public TaskObservable taskObservable() {
        if (observable == null) {
            observable = new TaskObservableImpl();
        }
        return observable;
    }

}
