package com.peak.profileservice.Configuration;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpConfiguration {

    @Bean
    public Storage googleCloudStorage() {
        // This automatically picks up your 'gcloud auth application-default login' credentials locally
        // And it will automatically pick up the IAM service account when deployed to Cloud Run/GCE/GKE!
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean(destroyMethod = "")
    public ExecutorProvider executorProvider(@Value("${quarantine.pubsub.worker-threads:2}")int configuredThreads) {
        int safeThreads = Math.max(1, Math.min(4, configuredThreads));
        return InstantiatingExecutorProvider.newBuilder()
                .setExecutorThreadCount(safeThreads)
                .build();
    }

    @Bean
    public FlowControlSettings flowControlSettings(@Value("${quarantine.pubsub.max-outstanding-elements:10}") long maxElements,
                                                   @Value("${quarantine.pubsub.max-outstanding-bytes:26214400}") long maxBytes)
    {
        return FlowControlSettings.newBuilder()
                .setMaxOutstandingElementCount(maxElements)
                .setMaxOutstandingRequestBytes(maxBytes)
                .build();
    }
}
