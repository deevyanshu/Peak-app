package com.peak.profileservice.GCPSubscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.peak.profileservice.Service.FileValidationService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class QuarantinePubSubSubscriber {

    private final FileValidationService fileValidationService;
    private final ObjectMapper objectMapper;
    // implementing executorprovider and flowcontrolsettings to limit the number of concurrent messages being processed and to avoid overwhelming the system with too many messages at once.
    private final ExecutorProvider executorProvider;
    private final FlowControlSettings flowControlSettings;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${gcp.pubsub.subscription:quarantine-local-sub}")
    private String subscriptionId;

    private Subscriber subscriber;

    @EventListener(ApplicationReadyEvent.class)
    public void startSubscriber(){
        ProjectSubscriptionName projectSubscriptionName= ProjectSubscriptionName.of(projectId, subscriptionId);
        MessageReceiver receiver=(PubsubMessage message, AckReplyConsumer consumer)->{
            try{
                String payloadJson=message.getData().toString(StandardCharsets.UTF_8);
                JsonNode gcsObject=objectMapper.readTree(payloadJson);
                String blobName=gcsObject.get("name").asText(null);
                if(blobName==null || !blobName.startsWith("pending/"))
                {
                    consumer.ack();
                    return;
                }

                String[] parts = blobName.split("/");
                if (parts.length < 2 || parts[1].isBlank()) {
                    consumer.ack();
                    return;
                }
                String userId = parts[1];
                fileValidationService.validateAndPromoteFile(message.getMessageId(), blobName, userId);
                consumer.ack();
            }catch (Exception ex){
                consumer.nack();
            }
        };
        this.subscriber = Subscriber.newBuilder(projectSubscriptionName, receiver).
        setExecutorProvider(this.executorProvider).
        setFlowControlSettings(this.flowControlSettings).
        build();
        this.subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    public void stopSubscriber() {
        if (subscriber != null) {
            subscriber.stopAsync().awaitTerminated();
        }
        if (this.executorProvider != null && this.executorProvider.getExecutor() != null) {
            this.executorProvider.getExecutor().shutdownNow();
        }
    }
}
