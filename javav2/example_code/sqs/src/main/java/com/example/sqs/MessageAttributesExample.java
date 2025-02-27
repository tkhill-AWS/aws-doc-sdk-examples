package com.example.sqs;

import org.slf4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageAttributesExample {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageAttributesExample.class);
    static final String QUEUE_NAME = "message-attribute-queue"+ UUID.randomUUID();
    static final SqsClient SQS_CLIENT = SqsClient.builder().httpClient(ApacheHttpClient.create()).build();

    public static void main(String[] args) throws IOException {
        String queueUrl = createQueue();
        try {
            Path thumbnailPath = Paths.get(MessageAttributesExample.class.getClassLoader().getResource("thumbnail.jpg").getPath());
            sendMessageWithAttributes(thumbnailPath, queueUrl);
            receiveAndDeleteMessages(queueUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            deleteQueue(queueUrl);
        }
    }

    static void sendMessageWithAttributes(Path thumbailPath, String queueUrl) throws IOException {
        Map<String, MessageAttributeValue> messageAttributeMap = Map.of(
                "Name", MessageAttributeValue.builder()
                        .stringValue("Jane Doe")
                        .dataType("String").build(),
                "Age", MessageAttributeValue.builder()
                        .stringValue("42")
                        .dataType("Number.int").build(),
                "Image", MessageAttributeValue.builder()
                        .binaryValue(SdkBytes.fromByteArray(Files.readAllBytes(thumbailPath)))
                        .dataType("Binary.jpg").build()
        );

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("Hello SQS")
                .messageAttributes(messageAttributeMap)
                .build();

        SendMessageResponse sendMessageResponse = SQS_CLIENT.sendMessage(request);
        LOGGER.info("Message ID: {}", sendMessageResponse.messageId());
    }

    private static void receiveAndDeleteMessages(String queueUrl) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1) // Retrieve one message at a time
                .waitTimeSeconds(10) // Enable long polling
                .messageAttributeNames("Name", "Age", "Image")
                .build();

        List<Message> messages = SQS_CLIENT.receiveMessage(request).messages();

        for (Message message : messages) {
            System.out.println("Received message: " + message.body());

            // Delete message after processing
            deleteMessage(queueUrl, message.receiptHandle());
        }
    }
    private static void deleteMessage(String queueUrl, String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();

        SQS_CLIENT.deleteMessage(request);
        System.out.println("Message deleted successfully.");
    }


    static String createQueue() {
       return SQS_CLIENT.createQueue(b -> b.queueName(QUEUE_NAME))
               .queueUrl();
    }
    static void deleteQueue(String queueUrl) {
        SQS_CLIENT.deleteQueue(b -> b.queueUrl(queueUrl));
    }

}
