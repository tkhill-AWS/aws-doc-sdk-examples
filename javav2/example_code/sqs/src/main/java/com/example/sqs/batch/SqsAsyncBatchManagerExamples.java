package com.example.sqs.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

public class SqsAsyncBatchManagerExamples {
    private static final Logger logger = LoggerFactory.getLogger(SqsAsyncBatchManagerExamples.class);
    static final String QUEUE_NAME = "my-queue" + UUID.randomUUID();
    static String queueUrl = null;
    static final SqsAsyncClient sqsAsyncClient = SqsAsyncClient.create();
    static final SqsAsyncBatchManager sqsAsyncBatchManager = sqsAsyncClient.batchManager();
    private static final Boolean CREATE_QUEUE = true;
    private static final Boolean DELETE_QUEUE = true;
    private static final Integer MESSAGE_COUNT = 100;
    private static Integer RECEIVED_COUNT = 0;

    public static void main(String[] args) {
        if (CREATE_QUEUE) {
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
        }
        sendMessages(MESSAGE_COUNT).join();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Integer loopCounter = 1;
        while (RECEIVED_COUNT < MESSAGE_COUNT) {
            logger.info("Loop countdown: {}", loopCounter);
            receiveMessages();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            loopCounter++;
        }

        if (DELETE_QUEUE) {
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
    }

    static CompletableFuture<Void> sendMessages(Integer count) {
            CompletableFuture[] futureMessageResponses = new CompletableFuture[count];
            IntStream.range(0, count).forEach(i -> {
                logger.info("sending single message {}", i);
                futureMessageResponses[i] = sendSingleMessage("Message " + i);
            });
            return CompletableFuture.allOf(futureMessageResponses);
    }

    static CompletableFuture<SendMessageResponse> sendSingleMessage(String messageBody){
        return sqsAsyncBatchManager.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageBody(messageBody));
    }

    static void receiveMessages(){
        sqsAsyncBatchManager.receiveMessage(r -> r
                        .queueUrl(queueUrl)
                )
                .whenComplete( (response, err) -> {
                    if (err != null) {
                        logger.error("Error sending message: {}", err.getCause().getMessage());
                    } else {
                        RECEIVED_COUNT += response.messages().size();
                        logger.info("Received {} messages", response.messages().size());
                        logger.info("Total messages received: {}", RECEIVED_COUNT);
                    }
                }).join();
    }

    static CompletableFuture<String> createQueue() {
        return sqsAsyncClient.createQueue(r -> r
                        .queueName(QUEUE_NAME)
                        .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, Integer.toString(5 * 60)
                         //       ,QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20")
        )))
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
