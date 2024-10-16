package com.example.sqs.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

public class SqsAsyncBatchManagerExamples {
    private static final Logger logger = LoggerFactory.getLogger(SqsAsyncBatchManagerExamples.class);
    static final String QUEUE_NAME = "my-queue" + UUID.randomUUID();
    static final Integer SCHEDULED_THREAD_POOL_SIZE = 3;
    static String queueUrl = null;
    static final SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder()
            .overrideConfiguration(b -> b
                    .addExecutionInterceptor(new TestInterceptor()
                    )
            ).build();
    static final SqsAsyncBatchManager sqsAsyncBatchManager = SqsAsyncBatchManager.builder()
            .client(sqsAsyncClient)
            .scheduledExecutor(Executors.newScheduledThreadPool(SCHEDULED_THREAD_POOL_SIZE))
            .overrideConfiguration(b -> b
                    .maxBatchSize(9)
                    .sendRequestFrequency(Duration.ofSeconds(1))
            ).build();
    private static final Boolean CREATE_QUEUE = true;
    private static final Boolean DELETE_QUEUE = true;
    private static final Integer MESSAGE_COUNT = 50;
    private static Integer RECEIVED_COUNT = 0;
    static List<String> receiptHandles = new ArrayList<>();
    static final Integer VISIBILITY_TIMEOUT = 200;

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
        sendMessages(MESSAGE_COUNT);
        try {
            Thread.sleep(45_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Integer loopCounter = 1;
        while (RECEIVED_COUNT < MESSAGE_COUNT) {
            logger.info("Loop countdown: {}", loopCounter);
            receiveMessages().join();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            loopCounter++;
        }

        changeVisiblityTimeout(receiptHandles, queueUrl, VISIBILITY_TIMEOUT);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
            } finally {
                sqsAsyncClient.close();
                sqsAsyncBatchManager.close();
            }
        }
        System.exit(0);
    }

    static void sendMessages(Integer count) {
         /*   CompletableFuture[] futureMessageResponses = new CompletableFuture[count];
            IntStream.range(0, count).forEach(i -> {
                logger.info("sending single message {}", i);
                futureMessageResponses[i] = sendSingleMessage("Message " + i);
            });
            return CompletableFuture.allOf(futureMessageResponses);
*/
        for (int i = 0; i < count; i++) {
            logger.info("sending single message {}", i);
            sendMyMessage("Message " + i, queueUrl);
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static CompletableFuture<SendMessageResponse> sendSingleMessage(String messageBody){
        return sqsAsyncBatchManager.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageBody(messageBody));
    }

    static void sendMyMessage(String messageBody, String queueUrl){
        sqsAsyncBatchManager.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageBody(messageBody))
                .whenComplete( (response, err) -> {
                    if (err != null) {
                        logger.error("Error sending message: {}", err.getCause().getMessage());
                    } else {
                        logger.info("Message sent successfully. Message ID: {}", response.messageId());
                    }
                });
    }

    static CompletableFuture<ReceiveMessageResponse> receiveMessages(){
        return sqsAsyncBatchManager.receiveMessage(r -> r
                        .queueUrl(queueUrl)
                )
                .whenComplete( (response, err) -> {
                    if (err != null) {
                        logger.error("Error sending message: {}", err.getCause().getMessage());
                    } else {
                        RECEIVED_COUNT += response.messages().size();
                        logger.info("Received {} messages", response.messages().size());
                        logger.info("Total messages received: {}", RECEIVED_COUNT);
                        for (Message singleResponse : response.messages()){
                            logger.info("ReceiptHandle: {}", singleResponse.receiptHandle());
                            receiptHandles.add(singleResponse.receiptHandle());
                        }
                    }
                });
    }

    static void changeVisiblityTimeout(List<String> receiptHandles, String queueUrl, Integer visibilityTimeout) {
        receiptHandles.forEach(rh -> {
            sqsAsyncBatchManager.changeMessageVisibility(r -> r
                            .receiptHandle(rh)
                            .queueUrl(queueUrl)
                            .visibilityTimeout(visibilityTimeout))
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            logger.error("Error changing visibility timeout: {}", err.getCause().getMessage());
                        } else {
                            logger.info("Visibility timeout changed successfully for {}", rh);
                        }
                    });
        });
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

    public static final class TestInterceptor implements ExecutionInterceptor {
        private static final Logger logger = LoggerFactory.getLogger(TestInterceptor.class);

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            if (context.request() instanceof SendMessageBatchRequest batchRequest){
                logger.info("Before batch send execution- sending {}", batchRequest.entries().size());
            } else if (context.request() instanceof ChangeMessageVisibilityBatchRequest changeVisibility) {
                logger.info("Before change visibility execution- sending {}", changeVisibility.entries().size());
            }
        }
    }
}
