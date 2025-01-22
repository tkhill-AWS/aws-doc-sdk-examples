// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.example.sqs;

// snippet-start:[sqs.java2.sqs_sse_example.main]
// snippet-start:[sqs.java2.sqs_sse_example.import]
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;
// snippet-end:[sqs.java2.sqs_sse_example.import]

/**
 * Before running this Java V2 code example, set up your development
 * environment, including your credentials.
 *
 * For more information, see the following documentation topic:
 *
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html
 */
public class SSEncryptionExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEncryptionExample.class);
    static final String STACK_NAME = "sqs-sse-example-stack";
    static final String CFN_TEMPLATE_FILE_NAME = "cfn_template.yaml";

    public static void main(String[] args) {
        CloudFormationHelper.deployCloudFormationStack(
                STACK_NAME, CFN_TEMPLATE_FILE_NAME);
        final Map<String, String> stackOutputs = CloudFormationHelper.getStackOutputs(STACK_NAME);

        String queueName = stackOutputs.get("QueueName");
        String kmsMasterKeyAlias = stackOutputs.get("KeyAlias");
        addEncryption(queueName, kmsMasterKeyAlias);
        CloudFormationHelper.destroyCloudFormationStack(STACK_NAME);
    }
    // snippet-start:[sqs.java2.sqs_sse_example.add-encryption-method]
    public static void addEncryption(String queueName, String kmsMasterKeyAlias) {
            SqsClient sqsClient = SqsClient.create();
            GetQueueUrlRequest urlRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(urlRequest);
            String queueUrl = getQueueUrlResponse.queueUrl();


            Map<QueueAttributeName, String> attributes = Map.of(
            QueueAttributeName.KMS_MASTER_KEY_ID, kmsMasterKeyAlias,
            QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS, "140"
            );

            SetQueueAttributesRequest attRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributes(attributes)
                    .build();
        try {
            sqsClient.setQueueAttributes(attRequest);
            LOGGER.info("The attributes have been applied to {}", queueName);
        } catch (SqsException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            sqsClient.close();
        }
    }
    // snippet-end:[sqs.java2.sqs_sse_example.add-encryption-method]
}
// snippet-end:[sqs.java2.sqs_sse_example.main]
