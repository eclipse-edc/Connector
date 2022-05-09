/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package com.siemens.mindsphere.provision;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemProvisionerTest {

    private static final String FILE_LOCATION = "file.txt";

    @AfterEach
    public void tearDown() {
        new File(FILE_LOCATION).delete();
    }

    @Test
    void givenFileResourceDefinition_whenProvision_checkCreatedFile() throws ExecutionException, InterruptedException, IOException {
        FileSystemProvisioner provisioner = new FileSystemProvisioner(new ConsoleMonitor(), new RetryPolicy<>());
        FileSystemResourceDefinition resourceDefinition = FileSystemResourceDefinition.Builder.newInstance()
                .path(FILE_LOCATION)
                .id("1234567890")
                .transferProcessId("11111111")
                .build();
        Policy policy = Policy.Builder.newInstance().build();
        StatusResult<ProvisionResponse> statusResult = provisioner.provision(resourceDefinition, policy).get();

        assertTrue(statusResult.succeeded());
        assertTrue(statusResult.getContent().getResource() instanceof FileSystemProvisionedResource);
        assertEquals(FILE_LOCATION, ((FileSystemProvisionedResource)statusResult.getContent().getResource()).getPath());
        assertTrue(Files.readAllLines(Path.of(((FileSystemProvisionedResource)statusResult.getContent().getResource()).getPath())).get(0).contains("Generated"));
    }
}