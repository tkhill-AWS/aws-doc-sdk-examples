package com.example.sqs.jms;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

public class JmsSqsSyncFifo {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JmsSqsSyncFifo.class);

    public static void main(String[] args) {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                SqsClient.create()
        );
        try {
            SQSConnection connection = connectionFactory.createConnection();


            AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
            if (!client.queueExists("MyQueue.fifo")) {
                Map<QueueAttributeName, String> attributes2 = Map.of(
                        QueueAttributeName.FIFO_QUEUE, "true",
                        QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true"
                );
                client.createQueue(CreateQueueRequest.builder()
                        .queueName("MyQueue.fifo")
                        .attributes(attributes2)
                        .build());
            }

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("MyQueue.fifo");
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage("Hello World!");
            message.setStringProperty("JMSXGroupID", "Default");
            producer.send(message);
            LOGGER.info("JMS Message {}", message.getJMSMessageID());
            LOGGER.info("JMS Message Sequence Number {}", message.getStringProperty("JMS_SQS_SequenceNumber"));

            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            Message receivedMessage = consumer.receive(1000);
            if (receivedMessage != null) {
                LOGGER.info("Received: {}", ((TextMessage) receivedMessage).getText());
                LOGGER.info("Group id: {}", receivedMessage.getStringProperty("JMSXGroupID"));
                LOGGER.info("Message deduplication id: {}", receivedMessage.getStringProperty("JMS_SQS_DeduplicationId"));
                LOGGER.info("Message sequence number: {}", receivedMessage.getStringProperty("JMS_SQS_SequenceNumber"));
            }
            connection.close();


        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
