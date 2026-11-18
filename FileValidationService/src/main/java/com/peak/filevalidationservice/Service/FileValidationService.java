package com.peak.filevalidationservice.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final Storage storage;

    @Value("${gcp.bucket.name}")
    private String productionBucket;

    @Value("${gcp.quarantine.bucket.name}")
    private String quarantineBucket;

    @Async("TaskExecutor")
    public void validateAndPromoteFile(String blobName, String userId) {
        BlobId sourceId = BlobId.of(quarantineBucket, blobName);
        Blob blob = storage.get(sourceId);

        if (blob == null || !blob.exists()) {
            return; // File not found in quarantine
        }

        byte[] contentBytes = blob.getContent();

        if (isMaliciousOrInvalidImage(contentBytes)) {
            // Security fail: delete fake/malicious file immediately
            storage.delete(sourceId);
            //send notification to be implemented
            return;
        }

        // Validation pass: Build target path using the userId
        String targetBlobName = "profiles/" + userId + "/avatar.jpg";
        BlobId targetId = BlobId.of(productionBucket, targetBlobName);

        // Copy to production public bucket
        storage.copy(Storage.CopyRequest.newBuilder().setSource(sourceId).setTarget(targetId).build());

        // Clean up quarantine file
        storage.delete(sourceId);

        // Save public file URL to PostgreSQL using the userId
        String finalPublicUrl = String.format("https://storage.googleapis.com/%s/%s", productionBucket, targetBlobName);
        // send using kafka
    }

    private boolean isMaliciousOrInvalidImage(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) return true;

        boolean isJpeg = (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF);
        boolean isPng = (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 && fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47);

        return !isJpeg && !isPng;
    }
}
