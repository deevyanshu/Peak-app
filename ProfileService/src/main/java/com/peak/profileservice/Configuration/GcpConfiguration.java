package com.peak.profileservice.Configuration;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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
}
