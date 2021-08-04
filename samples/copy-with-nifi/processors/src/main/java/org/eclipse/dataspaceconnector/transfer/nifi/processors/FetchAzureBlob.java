/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.nifi.processors;

import com.amazonaws.util.StringUtils;
import com.azure.core.http.policy.TimeoutPolicy;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Tags({"azure", "microsoft", "cloud", "storage", "blob"})
@CapabilityDescription("Retrieves contents of an Azure Storage Blob, either by listing an entire container if no files are specified, " +
        "or by obtaining a given set of files, writing the contents to the content of the FlowFile")
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@WritesAttributes({
        @WritesAttribute(attribute = "azure.length", description = "The length of the blob fetched")
})
public class FetchAzureBlob extends AbstractProcessor {

    private List<PropertyDescriptor> propertyDescriptors;
    private Set<Relationship> relationships;

    public FetchAzureBlob() {
        //needed by the ServiceLoader mechanism
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propertyDescriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected void init(ProcessorInitializationContext context) {
        List<PropertyDescriptor> descriptors = new ArrayList<>(Properties.FetchAzureBlob.Properties);

        propertyDescriptors = Collections.unmodifiableList(descriptors);
        relationships = Properties.FetchAzureBlob.Relationships;
    }


    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        long startNanos = System.nanoTime();

        String containerName = context.getProperty(Properties.CONTAINER).evaluateAttributeExpressions(flowFile).getValue();
        String blobNamesJsonArray = context.getProperty(Properties.OBJECT_KEYS).evaluateAttributeExpressions(flowFile).getValue();
        String sasToken = context.getProperty(Properties.SAS_TOKEN).evaluateAttributeExpressions(flowFile).getValue();
        String accountName = context.getProperty(Properties.ACCOUNT_NAME).evaluateAttributeExpressions(flowFile).getValue();


        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .sasToken(sasToken)
                .endpoint("https://" + accountName + ".blob.core.windows.net")
                .addPolicy(new TimeoutPolicy(Duration.ofSeconds(5)))
                .buildClient();

        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        if (!blobContainerClient.exists()) {
            getLogger().error("Container {} does not exist!", containerName);
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, Properties.REL_FAILURE);
            return;
        }

        List<String> names;
        if (StringUtils.isNullOrEmpty(blobNamesJsonArray)) {
            //fetch all
            names = blobContainerClient.listBlobs().stream().map(BlobItem::getName).collect(Collectors.toList());
        } else {
            try {
                names = new ObjectMapper().readValue(blobNamesJsonArray, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                getLogger().info("Could not interpret file names as JSON list - interpreting as single file name.");
                names = Collections.singletonList(blobNamesJsonArray);
            }
        }

        for (String blobName : names) {
            FlowFile ff = session.clone(flowFile);
            AtomicReference<Exception> storedException = new AtomicReference<>();
            try {

                BlobClient blob = blobContainerClient.getBlobClient(blobName);
                Map<String, String> attributes = new HashMap<>();

                attributes.put("filename", blobName);

                ff = session.write(ff, os -> {
                    try {
                        blob.downloadStream(os);
                    } catch (BlobStorageException e) {
                        storedException.set(e);
                        throw new IOException(e);
                    }
                });

                long length = blob.getProperties().getBlobSize();
                attributes.put("azure.length", String.valueOf(length));

                if (!attributes.isEmpty()) {
                    ff = session.putAllAttributes(ff, attributes);
                }

                session.transfer(ff, Properties.REL_SUCCESS);
                long transferMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                session.getProvenanceReporter().fetch(ff, blob.getBlobUrl(), transferMillis);

            } catch (IllegalArgumentException | BlobStorageException | ProcessException e) {
                if (e instanceof ProcessException && storedException.get() == null) {
                    throw e;
                } else {
                    Exception failureException = Optional.ofNullable(storedException.get()).orElse(e);
                    getLogger().error("Failure to fetch Azure blob {}", new Object[]{blobName}, failureException);
                    ff = session.penalize(ff);
                    session.transfer(ff, Properties.REL_FAILURE);
                }
            }
        }
        session.remove(flowFile);
    }
}
