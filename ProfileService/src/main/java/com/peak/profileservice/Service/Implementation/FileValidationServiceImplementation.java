package com.peak.profileservice.Service.Implementation;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.peak.profileservice.Service.FileValidationService;
import com.peak.profileservice.Service.ProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FileValidationServiceImplementation implements FileValidationService {

    private final Storage storage;

    private final ProfileService profileService;

    private static final Duration DEDUP_TTL=Duration.ofDays(7);
    private static final String KEY_PREFIX="pubsub:processed";
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${gcp.bucket.name}")
    private String productionBucket;

    @Value("${gcp.quarantine.bucket.name}")
    private String quarantineBucket;

    @Override
    @Transactional
    public void validateAndPromoteFile(String messageId, String blobName, String userId) {
        String dedupKey = (messageId != null && !messageId.isBlank())
                ? KEY_PREFIX + messageId
                : null;

//        if(messageId!=null && pubsubEventsRepository.existsById(messageId)){
//            return; // Duplicate message, already processed
//        }

        if(dedupKey!=null)
        {
            Boolean acquired=redisTemplate.opsForValue().setIfAbsent(dedupKey, blobName,DEDUP_TTL);
            if(Boolean.FALSE.equals(acquired))
            {
                return; // Duplicate message, already processed
            }
        }

        try{
            BlobId sourceId = BlobId.of(quarantineBucket, blobName);
            Blob blob = storage.get(sourceId);

            if (blob == null || !blob.exists()) {
                //recordProcessedEvent(messageId, blobName);
                return; // File not found in quarantine

            }

            byte[] contentBytes = blob.getContent();

            if (isMaliciousOrInvalidImage(contentBytes)) {
                // Security fail: delete fake/malicious file immediately
                storage.delete(sourceId);
                //recordProcessedEvent(messageId, blobName);
                //send notification to be implemented
                return;
            }

            // Validation pass: Build target path using the userId
            String[] parts = blobName.split("/");
            String targetBlobName = "profiles/" + userId + "/" + parts[parts.length - 1];
            BlobId targetId = BlobId.of(productionBucket, targetBlobName);

            // Copy to production public bucket
            storage.copy(Storage.CopyRequest.newBuilder().setSource(sourceId).setTarget(targetId).build());

            // Clean up quarantine file
            storage.delete(sourceId);

            // Save public file URL to PostgreSQL using the userId
            String finalPublicUrl = String.format("https://storage.googleapis.com/%s/%s", productionBucket, targetBlobName);
            profileService.saveProfilePic(userId, finalPublicUrl);
        }catch (Exception ex){
            if(dedupKey!=null)
            {
                redisTemplate.delete(dedupKey);
            }
            throw ex;
        }

        //recordProcessedEvent(messageId, blobName);
    }

    private boolean isMaliciousOrInvalidImage(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) return true;

        boolean isJpeg = (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF);
        boolean isPng = (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 && fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47);

        return !isJpeg && !isPng;
    }

//    private void recordProcessedEvent(String messageId, String blobName) {
//        if (messageId == null || messageId.isBlank()) {
//            return;
//        }
//        try {
//            pubsubEventsRepository.saveAndFlush(new ProcessPubsubEvents(messageId, blobName));
//        } catch (DataIntegrityViolationException ignored) {
//            // Another retry already inserted this messageId — safe to ignore
//        }
//    }
}
