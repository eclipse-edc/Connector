/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.StorageClass;
import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SupportsBatching
@SeeAlso({FetchS3Object.class})
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"Amazon", "S3", "AWS", "Archive", "Put"})
@CapabilityDescription("Puts FlowFiles to an Amazon S3 Bucket.\n" +
        "The upload uses either the PutS3Object method, no multipart upload is implemented as of yet.")
@DynamicProperty(name = "The name of a User-Defined Metadata field to add to the S3 Object",
        value = "The value of a User-Defined Metadata field to add to the S3 Object",
        description = "Allows user-defined metadata to be added to the S3 object as key/value pairs",
        expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
@WritesAttributes({
        @WritesAttribute(attribute = "s3.bucket", description = "The S3 bucket where the Object was put in S3"),
        @WritesAttribute(attribute = "s3.key", description = "The S3 key within where the Object was put in S3"),
        @WritesAttribute(attribute = "s3.contenttype", description = "The S3 content type of the S3 Object that put in S3"),
        @WritesAttribute(attribute = "s3.version", description = "The version of the S3 Object that was put to S3"),
        @WritesAttribute(attribute = "s3.etag", description = "The ETag of the S3 Object"),
        @WritesAttribute(attribute = "s3.contentdisposition", description = "The content disposition of the S3 Object that put in S3"),
        @WritesAttribute(attribute = "s3.cachecontrol", description = "The cache-control header of the S3 Object"),
        @WritesAttribute(attribute = "s3.uploadId", description = "The uploadId used to upload the Object to S3"),
        @WritesAttribute(attribute = "s3.expiration", description = "A human-readable form of the expiration date of " +
                "the S3 object, if one is set"),
        @WritesAttribute(attribute = "s3.sseAlgorithm", description = "The server side encryption algorithm of the object"),
        @WritesAttribute(attribute = "s3.usermetadata", description = "A human-readable form of the User Metadata of " +
                "the S3 object, if any was set"),
        @WritesAttribute(attribute = "s3.encryptionStrategy", description = "The name of the encryption strategy, if any was set"),})
public class PutS3Object extends AbstractProcessor {
    public static final String CONTENT_DISPOSITION_INLINE = "inline";
    public static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    final static String S3_PROCESS_UNSCHEDULED_MESSAGE = "Processor unscheduled, stopping upload";
    final static String S3_BUCKET_KEY = "s3.bucket";
    final static String S3_OBJECT_KEY = "s3.key";
    final static String S3_CONTENT_TYPE = "s3.contenttype";
    final static String S3_CONTENT_DISPOSITION = "s3.contentdisposition";
    final static String S3_VERSION_ATTR_KEY = "s3.version";
    final static String S3_ETAG_ATTR_KEY = "s3.etag";
    final static String S3_CACHE_CONTROL = "s3.cachecontrol";
    final static String S3_EXPIRATION_ATTR_KEY = "s3.expiration";
    final static String S3_STORAGECLASS_ATTR_KEY = "s3.storeClass";
    final static String S3_USERMETA_ATTR_KEY = "s3.usermetadata";
    final static String S3_API_METHOD_ATTR_KEY = "s3.apimethod";
    final static String S3_API_METHOD_PUTOBJECT = "putobject";

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final long startNanos = System.nanoTime();

        final String bucket = context.getProperty(Properties.BUCKET).evaluateAttributeExpressions(flowFile).getValue();
        final String key = context.getProperty(Properties.OBJECT_KEYS).evaluateAttributeExpressions(flowFile).getValue();
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

        final FlowFile ff = flowFile;
        final Map<String, String> attributes = new HashMap<>();
        final String ffFilename = ff.getAttributes().get(CoreAttributes.FILENAME.key());
        attributes.put(S3_BUCKET_KEY, bucket);
        attributes.put(S3_OBJECT_KEY, key);

        try {
            session.read(flowFile, rawIn -> {

                final ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(ff.getSize());
//                final String contentType = context.getProperty(CONTENT_TYPE).evaluateAttributeExpressions(ff).getValue();
//                if (contentType != null) {
//                    objectMetadata.setContentType(contentType);
//                    attributes.put(S3_CONTENT_TYPE, contentType);
//                }
//
//                final String cacheControl = context.getProperty(CACHE_CONTROL).evaluateAttributeExpressions(ff).getValue();
//                if (cacheControl != null) {
//                    objectMetadata.setCacheControl(cacheControl);
//                    attributes.put(S3_CACHE_CONTROL, cacheControl);
//                }
//
//                final String contentDisposition = context.getProperty(CONTENT_DISPOSITION).getValue();
//                String fileName = URLEncoder.encode(ff.getAttribute(CoreAttributes.FILENAME.key()), StandardCharsets.UTF_8);
//                if (contentDisposition != null && contentDisposition.equals(CONTENT_DISPOSITION_INLINE)) {
//                    objectMetadata.setContentDisposition(CONTENT_DISPOSITION_INLINE);
//                    attributes.put(S3_CONTENT_DISPOSITION, CONTENT_DISPOSITION_INLINE);
//                } else if (contentDisposition != null && contentDisposition.equals(CONTENT_DISPOSITION_ATTACHMENT)) {
//                    String contentDispositionValue = CONTENT_DISPOSITION_ATTACHMENT + "; filename=\"" + fileName + "\"";
//                    objectMetadata.setContentDisposition(contentDispositionValue);
//                    attributes.put(S3_CONTENT_DISPOSITION, contentDispositionValue);
//                } else {
//                    objectMetadata.setContentDisposition(fileName);
//                }

//                final String expirationRule = context.getProperty(EXPIRATION_RULE_ID).evaluateAttributeExpressions(ff).getValue();
//                if (expirationRule != null) {
//                    objectMetadata.setExpirationTimeRuleId(expirationRule);
//                }
                final Map<String, String> userMetadata = new HashMap<>();
                for (final Map.Entry<PropertyDescriptor, String> entry : context.getProperties().entrySet()) {
                    if (entry.getKey().isDynamic()) {
                        final String value = context.getProperty(
                                entry.getKey()).evaluateAttributeExpressions(ff).getValue();
                        userMetadata.put(entry.getKey().getName(), value);
                    }
                }

                if (!userMetadata.isEmpty()) {
                    objectMetadata.setUserMetadata(userMetadata);
                }

                final PutObjectRequest request = new PutObjectRequest(bucket, key, rawIn, objectMetadata);

                try {
                    final PutObjectResult result = s3Client.putObject(request);
                    if (result.getVersionId() != null) {
                        attributes.put(S3_VERSION_ATTR_KEY, result.getVersionId());
                    }
                    if (result.getETag() != null) {
                        attributes.put(S3_ETAG_ATTR_KEY, result.getETag());
                    }
                    if (result.getExpirationTime() != null) {
                        attributes.put(S3_EXPIRATION_ATTR_KEY, result.getExpirationTime().toString());
                    }
                    if (result.getMetadata().getStorageClass() != null) {
                        attributes.put(S3_STORAGECLASS_ATTR_KEY, result.getMetadata().getStorageClass());
                    } else {
                        attributes.put(S3_STORAGECLASS_ATTR_KEY, StorageClass.Standard.toString());
                    }
                    if (userMetadata.size() > 0) {
                        StringBuilder userMetaBldr = new StringBuilder();
                        for (String userKey : userMetadata.keySet()) {
                            userMetaBldr.append(userKey).append("=").append(userMetadata.get(userKey));
                        }
                        attributes.put(S3_USERMETA_ATTR_KEY, userMetaBldr.toString());
                    }
                    attributes.put(S3_API_METHOD_ATTR_KEY, S3_API_METHOD_PUTOBJECT);
                } catch (AmazonClientException e) {
                    getLogger().info("Failure completing upload flowfile={} bucket={} key={} reason={}",
                            ffFilename, bucket, key, e.getMessage());
                    throw (e);
                }
            });

            if (!attributes.isEmpty()) {
                flowFile = session.putAllAttributes(flowFile, attributes);
            }
            session.transfer(flowFile, Properties.REL_SUCCESS);

            final URL url = s3Client.getUrl(bucket, key);
            final long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            session.getProvenanceReporter().send(flowFile, url.toString(), millis);

            getLogger().info("Successfully put {} to Amazon S3 in {} milliseconds", ff, millis);
        } catch (final ProcessException | AmazonClientException pe) {
            if (pe.getMessage().contains(S3_PROCESS_UNSCHEDULED_MESSAGE)) {
                getLogger().info(pe.getMessage());
                session.rollback();
            } else {
                getLogger().error("Failed to put {} to Amazon S3 due to {}", flowFile, pe);
                flowFile = session.penalize(flowFile);
                session.transfer(flowFile, Properties.REL_FAILURE);
            }
        }
    }

    @Override
    public Set<Relationship> getRelationships() {
        return Properties.PutS3.Relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return Properties.PutS3.Properties;
    }
}
