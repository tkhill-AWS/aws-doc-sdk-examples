// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.example.sqs;
// snippet-start:[sqs.java2.tag-examples]

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;
import java.util.UUID;

public class TagExamples {
    static final SqsClient sqsClient = SqsClient.create();
    static final String queueName = "TagExamples-queue-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    private static final Logger LOGGER = LoggerFactory.getLogger(TagExamples.class);

    public static void main(String[] args) {
        final String queueUrl = sqsClient.createQueue(b -> b.queueName(queueName)).queueUrl();
        try {
            addTags(queueUrl);
            listTags(queueUrl);
            removeTags(queueUrl);
        } catch (SqsException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            sqsClient.deleteQueue(b -> b.queueUrl(queueUrl));
            sqsClient.close();
        }
    }

    // snippet-start:[sqs.java2.add-tags]
    public static void addTags(String queueUrl) {
        // Build a map of the tags.
        final Map<String, String> tagsToAdd = Map.of(
                "Team", "Development",
                "Priority", "Beta",
                "Accounting ID", "456def");

        // Add tags to the queue using a Consumer<TagQueueRequest.Builder> parameter.
        sqsClient.tagQueue(b -> b
                .queueUrl(queueUrl)
                .tags(tagsToAdd)
        );
    }
    // snippet-end:[sqs.java2.add-tags]
    // snippet-start:[sqs.java2.list-tags]
    public static void listTags(String queueUrl) {
        // Call the listQueueTags method with a Consumer<ListQueueTagsRequest.Builder> parameter that creates a ListQueueTagsRequest.
        ListQueueTagsResponse response = sqsClient.listQueueTags(b -> b
                .queueUrl(queueUrl));

        // Log the tags.
        response.tags()
                .forEach((k, v) ->
                        LOGGER.info("Key: {} -> Value: {}", k, v));
    }
    // snippet-end:[sqs.java2.list-tags]
    // snippet-start:[sqs.java2.remove-tags]
    public static void removeTags(String queueUrl) {
        // Call the untagQueue method with a Consumer<UntagQueueRequest.Builder> parameter.
        sqsClient.untagQueue(b -> b
                .queueUrl(queueUrl)
                .tagKeys("Accounting ID")
        );
    }
}
// snippet-start:[sqs.java2.remove-tags]
// snippet-end:[sqs.java2.tag-examples]