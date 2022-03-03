#
#  Copyright (c) 2021 Daimler TSS GmbH
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Daimler TSS GmbH - Initial API and Implementation
#       Fraunhofer Institute for Software and Systems Engineering - added environment variables
#
#

FROM gradle:7.2.0-jdk11-hotspot AS build

COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project/
RUN gradle :launchers:ids-connector:build --no-daemon

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=build /home/gradle/project/launchers/ids-connector/build/libs/dataspace-connector.jar /app

ENV EDC_FS_CONFIG=$EDC_FS_CONFIG
ENV EDC_VAULT=$EDC_VAULT
ENV EDC_KEYSTORE=$EDC_KEYSTORE
ENV EDC_KEYSTORE_PASSWORD=$EDC_KEYSTORE_PASSWORD

EXPOSE 8181

ENTRYPOINT java \
    -Djava.security.edg=file:/dev/.urandom \
    -Dedc.ids.id="urn:connector:edc-connector-24" \
    -Dedc.ids.title="Eclipse Dataspace Connector" \
    -Dedc.ids.description="Eclipse Dataspace Connector with IDS extensions" \
    -Dedc.ids.maintainer="https://example.maintainer.com" \
    -Dedc.ids.curator="https://example.maintainer.com" \
    -jar dataspace-connector.jar
