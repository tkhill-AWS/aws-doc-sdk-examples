package com.example.sqs.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

public class SqsAsyncBatchManagerExamples {
    private static final Logger logger = LoggerFactory.getLogger(SqsAsyncBatchManagerExamples.class);
    static final String QUEUE_NAME = "my-queue" + UUID.randomUUID();
    static String queueUrl = null;
    static final SqsAsyncClient sqsAsyncClient = SqsAsyncClient.create();
    static final SqsAsyncBatchManager sqsAsyncBatchManager = sqsAsyncClient.batchManager();

    public static void main(String[] args) {
        try {
            queueUrl = createQueue().join();
            logger.info("Queue {} created successfully with url {}", QUEUE_NAME, queueUrl);
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof QueueDeletedRecentlyException) {
                logger.error("Queue recently deleted: {}", cause.getMessage(), cause);
            } else {
                logger.error("Error creating queue: {}", cause.getMessage(), cause);
            }
            return;
        }
        sendMessages().join();
        try {
            deleteQueue().join();
            logger.info("Queue {} deleted successfully", QUEUE_NAME);
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof QueueDoesNotExistException) {
                logger.error("Queue does not exist: {}", ce.getCause().getMessage(), cause);
            }
        }
    }

    static CompletableFuture<Void> sendMessages() {
        return CompletableFuture.runAsync(() ->
                        IntStream.rangeClosed(1, 100).forEach(i ->
                                sqsAsyncBatchManager.sendMessage(r -> r
                                        .queueUrl(queueUrl)
                                        .messageBody("Message " + i)))
                )
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        logger.error("Error sending messages: {}", err.getCause().getMessage());
                    }
                });
    }



    static CompletableFuture<String> createQueue() {
        return sqsAsyncClient.createQueue(r -> r.queueName(QUEUE_NAME))
                .handle((resp, err) -> {
                    if (err != null) {
                        logger.error("Error creating queue: {}", err.getCause().getMessage());
                        throw (CompletionException) err;
                    } else {
                        return resp.queueUrl();
                    }
                });
    }

    static CompletableFuture<DeleteQueueResponse> deleteQueue() {
        return sqsAsyncClient.deleteQueue(r -> r.queueUrl(queueUrl))
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        logger.error("Error deleting queue: {}", err.getCause().getMessage());
                    }
                });
    }
}
