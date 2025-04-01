package uk.gov.dwp.health.pip.identity.event.utils;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageUtils {

  private final AmazonSNS amazonSNS;
  private final AmazonSQS amazonSQS;
  private final String requestTopicArn;
  private final String requestRoutingKey;
  private final String requestQueueUrl;
  private final String deadLetterQueueUrl;
  private final String pipCsResponseQueueUrl;

  private final String pipServiceResponseQueueUrl;

  public MessageUtils(
      String serviceEndpoint,
      String awsRegion,
      String requestTopicArn,
      String requestRoutingKey,
      String requestQueueUrl,
      String deadLetterQueueUrl,
      String pipCsResponseQueueUrl,
      String pipServiceResponseQueueUrl
      ) {
    var endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, awsRegion);

    amazonSNS =
        AmazonSNSClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).build();

    amazonSQS =
        AmazonSQSClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).build();

    this.requestTopicArn = requestTopicArn;
    this.requestRoutingKey = requestRoutingKey;
    this.requestQueueUrl = requestQueueUrl;
    this.deadLetterQueueUrl = deadLetterQueueUrl;
    this.pipCsResponseQueueUrl = pipCsResponseQueueUrl;
    this.pipServiceResponseQueueUrl = pipServiceResponseQueueUrl;
  }

  public void publishMessage(String message) {
    Map<String, MessageAttributeValue> messageAttributes = createMessageAttributes();

    var publishRequest = new PublishRequest();
    publishRequest.setTopicArn(requestTopicArn);
    publishRequest.setMessageAttributes(messageAttributes);
    publishRequest.setMessage(message);
    amazonSNS.publish(publishRequest);
  }

  private Map<String, MessageAttributeValue> createMessageAttributes() {
    Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

    var routingMessageAttribute = new MessageAttributeValue();
    routingMessageAttribute.setStringValue(requestRoutingKey);
    routingMessageAttribute.setDataType("String");
    messageAttributes.put("x-dwp-pip_identity_routing_key", routingMessageAttribute);

    var createdMessageAttribute = new MessageAttributeValue();
    createdMessageAttribute.setStringValue(DateTime.now().toString());
    createdMessageAttribute.setDataType("String");
    messageAttributes.put("x-dwp-event-created-dt", createdMessageAttribute);

    var versionMessageAttribute = new MessageAttributeValue();
    versionMessageAttribute.setStringValue("0.1");
    versionMessageAttribute.setDataType("String");
    messageAttributes.put("x-dwp-version", versionMessageAttribute);

    var eventMessageAttribute = new MessageAttributeValue();
    eventMessageAttribute.setStringValue("PipcsRequestEvent");
    eventMessageAttribute.setDataType("String");
    messageAttributes.put("x-dwp-event-header", eventMessageAttribute);

    var correlationMessageAttribute = new MessageAttributeValue();
    correlationMessageAttribute.setStringValue(UUID.randomUUID().toString());
    correlationMessageAttribute.setDataType("String");
    messageAttributes.put("x-dwp-correlation-id", correlationMessageAttribute);

    return messageAttributes;
  }

  private Message getMessage(String queueUrl) {
    var receiveMessageRequest =
        new ReceiveMessageRequest(queueUrl)
            .withMessageAttributeNames("All")
            .withMaxNumberOfMessages(1);
    var receiveMessageResult = amazonSQS.receiveMessage(receiveMessageRequest);

    return receiveMessageResult.getMessages().get(0);
  }

  public Message getPipCsResponseMessage() {
    return getMessage(pipCsResponseQueueUrl);
  }

  public Message getPipServiceResponseMessage() {
    return getMessage(pipServiceResponseQueueUrl);
  }

  public Message getDlqMessage() {
    return getMessage(deadLetterQueueUrl);
  }

  private void purgeQueue(String queueUrl) {
    amazonSQS.purgeQueue(new PurgeQueueRequest(queueUrl));
  }

  public void purgeRequestQueue() {
    purgeQueue(requestQueueUrl);
  }

  public void purgePipCsResponseQueue() {
    purgeQueue(pipCsResponseQueueUrl);
  }
  public void purgePipServiceResponseQueue() {
    purgeQueue(pipServiceResponseQueueUrl);
  }

  public void purgeDeadLetterQueue() {
    purgeQueue(deadLetterQueueUrl);
  }

  private Map<String, String> getQueueAttributes(String queueUrl) {
    var getQueueAttributesRequest =
        new GetQueueAttributesRequest(queueUrl).withAttributeNames(QueueAttributeName.All);
    return amazonSQS.getQueueAttributes(getQueueAttributesRequest).getAttributes();
  }

  public String getRequestMessageCount() {
    var queueAttributes = getQueueAttributes(requestQueueUrl);
    return queueAttributes.get(QueueAttributeName.ApproximateNumberOfMessages.toString());
  }

  public String getPipCsResponseMessageCount() {
    var queueAttributes = getQueueAttributes(pipCsResponseQueueUrl);
    return queueAttributes.get(QueueAttributeName.ApproximateNumberOfMessages.toString());
  }

  public String getPipServiceResponseMessageCount() {
    var queueAttributes = getQueueAttributes(pipServiceResponseQueueUrl);
    return queueAttributes.get(QueueAttributeName.ApproximateNumberOfMessages.toString());
  }

  public String getDlqMessageCount() {
    var queueAttributes = getQueueAttributes(deadLetterQueueUrl);
    return queueAttributes.get(QueueAttributeName.ApproximateNumberOfMessages.toString());
  }
}
