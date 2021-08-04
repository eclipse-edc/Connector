/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.nifi.processors;

import com.amazonaws.regions.Regions;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Properties {

    public static final long MIN_S3_PART_SIZE = 50L * 1024L * 1024L;
    public static final long MAX_S3_PUTOBJECT_SIZE = 5L * 1024L * 1024L * 1024L;
    public static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    public static final String CONTENT_DISPOSITION_INLINE = "inline";


    public static final PropertyDescriptor BUCKET = new PropertyDescriptor.Builder()
            .name("Bucket")
            .description("The bucket name")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor OBJECT_KEYS = new PropertyDescriptor.Builder()
            .name("Object Key")
            .description("A JSON-array of file names of objects that are to be fetched.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .build();
    public static final PropertyDescriptor TIMEOUT = new PropertyDescriptor.Builder()
            .name("Communications Timeout")
            .required(true)
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .defaultValue("30 secs")
            .build();
    public static final PropertyDescriptor ACCESS_KEY_ID = new PropertyDescriptor
            .Builder().name("Access Key ID")
            .displayName("Access Key ID")
            .description("Access Key ID for AWS")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .sensitive(true)
            .build();
    public static final PropertyDescriptor SECRET_ACCESS_KEY = new PropertyDescriptor
            .Builder().name("Secret Access Key")
            .displayName("Secret Access Key")
            .description("Secret Access Key for AWS")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .sensitive(true)
            .build();

    public static final PropertyDescriptor SESSION_TOKEN = new PropertyDescriptor
            .Builder().name("Session Token (STS)")
            .displayName("Session Token (STS)")
            .description("If a temporary role/policy was created, an STS can be supplied to assume that role")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .sensitive(true)
            .build();
    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
            .description("FlowFiles are routed to success relationship").build();
    public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
            .description("FlowFiles are routed to failure relationship").build();
    public static final PropertyDescriptor CONTENT_TYPE = new PropertyDescriptor.Builder()
            .name("Content Type")
            .displayName("Content Type")
            .description("Sets the Content-Type HTTP header indicating the type of content stored in the associated " +
                    "object. The value of this header is a standard MIME type.\n" +
                    "AWS S3 Java client will attempt to determine the correct content type if one hasn't been set" +
                    " yet. Users are responsible for ensuring a suitable content type is set when uploading streams. If " +
                    "no content type is provided and cannot be determined by the filename, the default content type " +
                    "\"application/octet-stream\" will be used.")
            .required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor CACHE_CONTROL = new PropertyDescriptor.Builder()
            .name("Cache Control")
            .displayName("Cache Control")
            .description("Sets the Cache-Control HTTP header indicating the caching directives of the associated object. Multiple directives are comma-separated.")
            .required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor CONTENT_DISPOSITION = new PropertyDescriptor.Builder()
            .name("Content Disposition")
            .displayName("Content Disposition")
            .description("Sets the Content-Disposition HTTP header indicating if the content is intended to be displayed inline or should be downloaded.\n " +
                    "Possible values are 'inline' or 'attachment'. If this property is not specified, object's content-disposition will be set to filename. " +
                    "When 'attachment' is selected, '; filename=' plus object key are automatically appended to form final value 'attachment; filename=\"filename.jpg\"'.")
            .required(false)
            .allowableValues(CONTENT_DISPOSITION_INLINE, CONTENT_DISPOSITION_ATTACHMENT)
            .build();

    public static final PropertyDescriptor EXPIRATION_RULE_ID = new PropertyDescriptor.Builder()
            .name("Expiration Time Rule")
            .required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor CONTAINER = new PropertyDescriptor.Builder()
            .name("container-name")
            .displayName("Container Name")
            .description("Name of the Azure storage container.")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .required(true)
            .build();
    public static final PropertyDescriptor SAS_TOKEN = new PropertyDescriptor.Builder()
            .name("storage-sas-token")
            .displayName("SAS Token")
            .description("Shared Access Signature token, including the leading '?'. Specify either SAS Token (recommended) or Account Key. " +
                    "There are certain risks in allowing the SAS token to be stored as a flowfile " +
                    "attribute. While it does provide for a more flexible flow by allowing the account name to " +
                    "be fetched dynamically from a flowfile attribute, care must be taken to restrict access to " +
                    "the event provenance data (e.g. by strictly controlling the policies governing provenance for this Processor). " +
                    "In addition, the provenance repositories may be put on encrypted disk partitions.")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor ACCOUNT_NAME = new PropertyDescriptor.Builder()
            .name("account-name")
            .displayName("Storage Account Name")
            .description("The name of the storage account that contains the container and for which the sas token is valid.")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .required(true)
            .sensitive(true)
            .build();
    private static final Set<String> ALLOWED_REGIONS = Stream.of(Regions.values()).map(Regions::getName).collect(Collectors.toSet());
    public static final PropertyDescriptor REGION = new PropertyDescriptor
            .Builder().name("Region")
            .displayName("Region")
            .description("The region where the bucket resides. If set incorrectly, AWS might not be able to retrieve the content")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .defaultValue(Regions.US_EAST_1.getName())
            .allowableValues(ALLOWED_REGIONS)
            .sensitive(false)
            .build();

    public static class FetchS3 {

        public static final Set<Relationship> Relationships = new HashSet<Relationship>() {{
            add(REL_SUCCESS);
            add(REL_FAILURE);
        }};
        public static List<PropertyDescriptor> Properties = Arrays.asList(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, SESSION_TOKEN, OBJECT_KEYS, BUCKET, TIMEOUT);
    }

    public static class PutS3 {
        public static final Set<Relationship> Relationships = new HashSet<Relationship>() {{
            add(REL_SUCCESS);
            add(REL_FAILURE);
        }};
        public static List<PropertyDescriptor> Properties = Arrays.asList(ACCESS_KEY_ID, SECRET_ACCESS_KEY, REGION, SESSION_TOKEN, OBJECT_KEYS, BUCKET, TIMEOUT);
    }

    public static class FetchAzureBlob {
        public static final List<PropertyDescriptor> Properties = Arrays.asList(CONTAINER, ACCOUNT_NAME, SAS_TOKEN, OBJECT_KEYS);
        public static final Set<Relationship> Relationships = new HashSet<Relationship>() {{
            add(REL_SUCCESS);
            add(REL_FAILURE);
        }};
    }
}
