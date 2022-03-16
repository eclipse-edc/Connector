/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.datafactory.models.CreateRunResponse;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import com.azure.resourcemanager.datafactory.models.PipelineRun;

/**
 * Client for Azure Data Factory, wrapping the Azure SDK.
 */
class DataFactoryClient {
    private final DataFactoryManager dataFactoryManager;
    private final String resourceGroupName;
    private final String factoryName;

    public DataFactoryClient(DataFactoryManager dataFactoryManager, String resourceGroupName, String factoryName) {
        this.dataFactoryManager = dataFactoryManager;
        this.resourceGroupName = resourceGroupName;
        this.factoryName = factoryName;
    }

    /**
     * Define a Data Factory pipeline.
     *
     * @param name pipeline name.
     * @return a pipeline definition.
     */
    PipelineResource.DefinitionStages.WithCreate definePipeline(String name) {
        return dataFactoryManager.pipelines()
                .define(name)
                .withExistingFactory(resourceGroupName, factoryName);
    }

    /**
     * Define a Data Factory linked service.
     *
     * @param name linked service name.
     * @return a linked service definition.
     */
    LinkedServiceResource.DefinitionStages.WithProperties defineLinkedService(String name) {
        return dataFactoryManager
                .linkedServices()
                .define(name)
                .withExistingFactory(resourceGroupName, factoryName);
    }

    /**
     * Define a Data Factory dataset.
     *
     * @param name dataset name.
     * @return a dataset definition.
     */
    DatasetResource.DefinitionStages.WithProperties defineDataset(String name) {
        return dataFactoryManager
                .datasets()
                .define(name)
                .withExistingFactory(resourceGroupName, factoryName);
    }


    /**
     * Runs a pipeline.
     *
     * @param pipeline pipeline to run.
     * @return run response.
     */
    CreateRunResponse runPipeline(PipelineResource pipeline) {
        return dataFactoryManager.pipelines()
                .createRun(resourceGroupName, factoryName, pipeline.name());
    }

    /**
     * Gets a pipeline run.
     *
     * @param runId pipeline run identifier.
     * @return run representation.
     */
    PipelineRun getPipelineRun(String runId) {
        return dataFactoryManager
                .pipelineRuns()
                .get(resourceGroupName, factoryName, runId);
    }

    /**
     * Cancels a pipeline run.
     *
     * @param runId pipeline run identifier.
     */
    void cancelPipelineRun(String runId) {
        dataFactoryManager
                .pipelineRuns()
                .cancel(resourceGroupName, factoryName, runId);
    }
}
