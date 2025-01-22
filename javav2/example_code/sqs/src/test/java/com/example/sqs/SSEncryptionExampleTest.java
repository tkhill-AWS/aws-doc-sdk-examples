package com.example.sqs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SSEncryptionExampleTest {

    @Mock
    private SqsClient sqsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test case for addEncryption method
     * Verifies that the method successfully adds encryption attributes to the SQS queue
     */
    @Test
    public void testAddEncryptionSuccessful() {
        // Arrange
        String queueName = "TestQueue";
        String kmsMasterKeyAlias = "test-kms-key";
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/TestQueue";

        SqsClient mockSqsClient = mock(SqsClient.class);
        GetQueueUrlResponse mockGetQueueUrlResponse = mock(GetQueueUrlResponse.class);

        try (MockedStatic<SqsClient> mockedStaticSqsClient = Mockito.mockStatic(SqsClient.class)) {
            mockedStaticSqsClient.when(SqsClient::create).thenReturn(mockSqsClient);

            when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(mockGetQueueUrlResponse);
            when(mockGetQueueUrlResponse.queueUrl()).thenReturn(queueUrl);

            // Act
            SSEncryptionExample.addEncryption(queueName, kmsMasterKeyAlias);

            // Assert
            verify(mockSqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
            verify(mockSqsClient).setQueueAttributes(any(SetQueueAttributesRequest.class));
            verify(mockSqsClient).close();
        }
    }

    @Test
    public void testAddEncryption_EmptyKmsMasterKeyAlias() {
        /**
         * Test case for empty KMS master key alias
         */
        assertThrows(IllegalArgumentException.class, () -> {
            SSEncryptionExample.addEncryption("validQueue", "");
        });
    }

    @Test
    public void testAddEncryption_EmptyQueueName() {
        /**
         * Test case for empty queue name
         */
        assertThrows(IllegalArgumentException.class, () -> {
            SSEncryptionExample.addEncryption("", "validKey");
        });
    }

    @Test
    public void testAddEncryption_InvalidKmsMasterKeyAlias() {
        /**
         * Test case for invalid KMS master key alias
         */
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
            .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://valid-url").build());

        when(sqsClient.setQueueAttributes(any(SetQueueAttributesRequest.class)))
            .thenThrow(InvalidAttributeNameException.class);

        assertThrows(InvalidAttributeNameException.class, () -> {
            SSEncryptionExample.addEncryption("validQueue", "invalidKey");
        });
    }

    @Test
    public void testAddEncryption_NullKmsMasterKeyAlias() {
        /**
         * Test case for null KMS master key alias
         */
        assertThrows(NullPointerException.class, () -> {
            SSEncryptionExample.addEncryption("validQueue", null);
        });
    }

    @Test
    public void testAddEncryption_NullQueueName() {
        /**
         * Test case for null queue name
         */
        assertThrows(NullPointerException.class, () -> {
            SSEncryptionExample.addEncryption(null, "validKey");
        });
    }

    @Test
    public void testAddEncryption_QueueNotFound() {
        /**
         * Test case for non-existent queue
         */
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
            .thenThrow(QueueDoesNotExistException.class);

        assertThrows(QueueDoesNotExistException.class, () -> {
            SSEncryptionExample.addEncryption("nonExistentQueue", "validKey");
        });
    }

    @Test
    public void testAddEncryption_SqsException() {
        /**
         * Test case for SQS exception
         */
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
            .thenReturn(GetQueueUrlResponse.builder().queueUrl("http://valid-url").build());

        when(sqsClient.setQueueAttributes(any(SetQueueAttributesRequest.class)))
            .thenThrow(SqsException.class);

        assertThrows(SqsException.class, () -> {
            SSEncryptionExample.addEncryption("validQueue", "validKey");
        });
    }

}