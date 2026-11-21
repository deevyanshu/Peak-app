package com.peak.profileservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_pubsub_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessPubsubEvents {

    @Column(name="message_id")
    @Id
    @NotNull
    @NotBlank
    private String messageId;

    @Column(name="blob_name", nullable = false)
    @NotNull
    @NotBlank
    private String blobName;

    @Column(name="processed_at",nullable = false)
    @NotNull
    private LocalDateTime processedAt= LocalDateTime.now();

    public ProcessPubsubEvents(String messageId, String blobName) {
        this.messageId = messageId;
        this.blobName = blobName;
        this.processedAt = LocalDateTime.now();
    }

}
