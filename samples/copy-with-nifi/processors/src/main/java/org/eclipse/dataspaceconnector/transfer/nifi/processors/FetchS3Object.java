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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.FlowFileAccessException;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@SupportsBatching
@SeeAlso({PutS3Object.class})
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"Amazon", "S3", "AWS", "Get", "Fetch"})
@CapabilityDescription("Retrieves the contents of one or many S3 Objects and produces one FlowFile per object.")
@WritesAttributes({
        @WritesAttribute(attribute = "s3.bucket", description = "The name of the S3 bucket"),
        @WritesAttribute(attribute = "path", description = "The path of the file"),
        @WritesAttribute(attribute = "absolute.path", description = "The path of the file"),
//        @WritesAttribute(attribute = "filename", description = "The name of the file"),
        @WritesAttribute(attribute = "hash.value", description = "The MD5 sum of the file"),
        @WritesAttribute(attribute = "hash.algorithm", description = "MD5"),
        @WritesAttribute(attribute = "mime.type", description = "If S3 provides the content type/MIME type, this attribute will hold that file"),
        @WritesAttribute(attribute = "s3.etag", description = "The ETag that can be used to see if the file has changed"),
        @WritesAttribute(attribute = "s3.expirationTime", description = "If the file has an expiration date, this attribute will be set, containing the milliseconds since epoch in UTC time"),
        @WritesAttribute(attribute = "s3.expirationTimeRuleId", description = "The ID of the rule that dictates this object's expiration time"),
        @WritesAttribute(attribute = "s3.sseAlgorithm", description = "The server side encryption algorithm of the object"),
        @WritesAttribute(attribute = "s3.version", description = "The version of the S3 object"),
        @WritesAttribute(attribute = "s3.encryptionStrategy", description = "The name of the encryption strategy that was used to store the S3 object (if it is encrypted)"),})
public class FetchS3Object extends AbstractProcessor {

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    public FetchS3Object() {
        //needed by the ServiceLoader mechanism
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>(Properties.FetchS3.Properties);

        this.descriptors = Collections.unmodifiableList(descriptors);
        relationships = Properties.FetchS3.Relationships;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        final long startNanos = System.nanoTime();

        final String bucket = context.getProperty(Properties.BUCKET).evaluateAttributeExpressions(flowFile).getValue();
        final String objectKeyJsonArray = context.getProperty(Properties.OBJECT_KEYS).evaluateAttributeExpressions(flowFile).getValue();
        final String region = context.getProperty(Properties.REGION).evaluateAttributeExpressions(flowFile).getValue();
        final String accessKeyId = context.getProperty(Properties.ACCESS_KEY_ID).evaluateAttributeExpressions(flowFile).getValue();
        final String secretKey = context.getProperty(Properties.SECRET_ACCESS_KEY).evaluateAttributeExpressions(flowFile).getValue();
        final String sessionToken = context.getProperty(Properties.SESSION_TOKEN).evaluateAttributeExpressions(flowFile).getValue();

        AWSCredentials credentials = sessionToken != null
                ? new BasicSessionCredentials(accessKeyId, secretKey, sessionToken)
                : new BasicAWSCredentials(accessKeyId, secretKey);


        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();

        List<String> names;
        if (StringUtils.isNullOrEmpty(objectKeyJsonArray)) {
            //fetch all
            names = s3Client.listObjects(bucket).getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
        } else {
            try {
                names = new ObjectMapper().readValue(objectKeyJsonArray, new TypeReference<List<String>>() {
                });
            } catch (JsonProcessingException e) {
                getLogger().info("Could not interpret file names as JSON list - interpreting as single file name.");
                names = Collections.singletonList(objectKeyJsonArray);
            }
        }


        for (String objectKey : names) {

            FlowFile ff = session.clone(flowFile);
            GetObjectRequest request = new GetObjectRequest(bucket, objectKey);
            final Map<String, String> attributes = new HashMap<>();

            try (final S3Object s3Object = s3Client.getObject(request)) {
                if (s3Object == null) {
                    throw new IOException("AWS refused to execute this request.");
                }
                session.importFrom(s3Object.getObjectContent(), ff);
                attributes.put("s3.bucket", bucket);

                ObjectMetadata metadata = s3Object.getObjectMetadata();

                if (metadata.getContentDisposition() != null) {
                    final String contentDisposition = metadata.getContentDisposition();

                    if (contentDisposition.equals("inline") || contentDisposition.startsWith("attachment; filename=")) {
                        setFilePathAttributes(attributes, objectKey);
                    } else {
                        setFilePathAttributes(attributes, contentDisposition);
                    }
                }

                attributes.put("filename", objectKey);

                if (metadata.getContentMD5() != null) {
                    attributes.put("hash.value", metadata.getContentMD5());
                    attributes.put("hash.algorithm", "MD5");
                }
                if (metadata.getContentType() != null) {
                    attributes.put(CoreAttributes.MIME_TYPE.key(), metadata.getContentType());
                }
                if (metadata.getETag() != null) {
                    attributes.put("s3.etag", metadata.getETag());
                }
                if (metadata.getExpirationTime() != null) {
                    attributes.put("s3.expirationTime", String.valueOf(metadata.getExpirationTime().getTime()));
                }
                if (metadata.getExpirationTimeRuleId() != null) {
                    attributes.put("s3.expirationTimeRuleId", metadata.getExpirationTimeRuleId());
                }
                if (metadata.getUserMetadata() != null) {
                    attributes.putAll(metadata.getUserMetadata());
                }
                if (metadata.getVersionId() != null) {
                    attributes.put("s3.version", metadata.getVersionId());
                }

            } catch (IOException | AmazonClientException | FlowFileAccessException ioe) {
                getLogger().error("Failed to retrieve S3 Object for {}; routing to failure", ff, ioe);
                ff = session.penalize(ff);
                session.transfer(ff, Properties.REL_FAILURE);
                continue;
            }

            if (!attributes.isEmpty()) {
                ff = session.putAllAttributes(ff, attributes);
            }

            session.transfer(ff, Properties.REL_SUCCESS);
            final long transferMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            getLogger().info("Successfully retrieved S3 Object for {} in {} millis; routing to success", ff, transferMillis);
            session.getProvenanceReporter().fetch(ff, "http://" + bucket + ".amazonaws.com/" + objectKey, transferMillis);
        }
        session.remove(flowFile);
    }

    protected void setFilePathAttributes(Map<String, String> attributes, String filePathName) {
        final int lastSlash = filePathName.lastIndexOf("/");
        if (lastSlash > -1 && lastSlash < filePathName.length() - 1) {
            attributes.put(CoreAttributes.PATH.key(), filePathName.substring(0, lastSlash));
            attributes.put(CoreAttributes.ABSOLUTE_PATH.key(), filePathName);
            attributes.put(CoreAttributes.FILENAME.key(), filePathName.substring(lastSlash + 1));
        } else {
            attributes.put(CoreAttributes.FILENAME.key(), filePathName);
        }
    }
}
