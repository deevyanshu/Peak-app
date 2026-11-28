package com.peak.profileservice.Service.Implementation;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.peak.profileservice.Entity.Profile;
import com.peak.profileservice.Service.FileValidationService;
import com.peak.profileservice.Service.ProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileValidationServiceImplementation implements FileValidationService {

    private final Storage storage;

    private final ProfileService profileService;

    private static final Duration DEDUP_TTL=Duration.ofDays(7);
    private static final String KEY_PREFIX="pubsub:processed";
    private final RedisTemplate<String, String> redisTemplate;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // 10 MB

    @Value("${gcp.bucket.name}")
    private String productionBucket;

    @Value("${gcp.quarantine.bucket.name}")
    private String quarantineBucket;

    @Override
    @Transactional
    public void validateAndPromoteFile(String messageId, String blobName, String userId) throws IOException {
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

            if (isOversized(blob)) {
                boolean deleted = storage.delete(sourceId);
                return;
            }

//            byte[] contentBytes = blob.getContent();
//
//            if (isMaliciousOrInvalidImage(contentBytes)) {
//                // Security fail: delete fake/malicious file immediately
//                storage.delete(sourceId);
//                //recordProcessedEvent(messageId, blobName);
//                //send notification to be implemented
//                return;
//            }
            //using this method to check the header of the image instead of reading the entire content to avoid memory issues with large files
            if(!hasValidImageHeader(blob)) {
                // Security fail: delete fake/malicious file immediately
                storage.delete(sourceId);
                return;
            }

            //compressing the image using thumbnailator
            ByteArrayOutputStream compressedOut=new ByteArrayOutputStream();
            try (InputStream in = Channels.newInputStream(blob.reader())) {
                Thumbnails.of(in)
                        .size(800, 800)          // Ideal max size for profile photos (keeps aspect ratio)
                        .outputFormat("jpg")
                        .outputQuality(0.80f)    // High clarity with strong size reduction
                        .toOutputStream(compressedOut);
            }

            // Validation pass: Build target path using the userId
            String[] parts = blobName.split("/");
            String targetBlobName = "profiles/" + userId + "/" + parts[parts.length - 1];
            BlobId targetId = BlobId.of(productionBucket, targetBlobName);
            BlobInfo targetInfo = BlobInfo.newBuilder(targetId)
                    .setContentType("image/jpeg")
                    //.setCacheControl("public, max-age=86400") // 24-hour cache (allows profile updates)
                    .build();
            // Copy to production public bucket
            //storage.copy(Storage.CopyRequest.newBuilder().setSource(sourceId).setTarget(targetId).build());
            storage.create(targetInfo,compressedOut.toByteArray());
            // Clean up quarantine file
            storage.delete(sourceId);

            // Save public file URL to PostgreSQL using the userId
            String finalPublicUrl = String.format("https://storage.googleapis.com/%s/%s", productionBucket, targetBlobName);
            String deleteUrl=profileService.getProfilePicUrl(userId);
            profileService.saveProfilePic(userId, finalPublicUrl);
            if(deleteUrl!=null && !deleteUrl.isBlank())
            {
                log.info("Deleting old profile picture: " + deleteUrl);
                deleteByPublicUrl(deleteUrl);
            }

        }catch (Exception ex){
            if(dedupKey!=null)
            {
                redisTemplate.delete(dedupKey);
            }
            throw ex;
        }

        //recordProcessedEvent(messageId, blobName);
    }

//    private boolean isMaliciousOrInvalidImage(byte[] fileBytes) {
//        if (fileBytes == null || fileBytes.length < 4) return true;
//
//        boolean isJpeg = (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF);
//        boolean isPng = (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 && fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47);
//
//        return !isJpeg && !isPng;
//    }

    private boolean isOversized(Blob blob) {
        Long size = blob.getSize();
        return size != null && size > MAX_IMAGE_BYTES;
    }

    private boolean hasValidImageHeader(Blob blob) {
        byte[] header = new byte[8];
        try (InputStream in = Channels.newInputStream(blob.reader())) {
            int bytesRead = in.readNBytes(header, 0, header.length);
            if (bytesRead < 4) {
                return false;
            }
            boolean isJpeg = (header[0] == (byte) 0xFF &&
                    header[1] == (byte) 0xD8 &&
                    header[2] == (byte) 0xFF);
            boolean isPng = (header[0] == (byte) 0x89 &&
                    header[1] == (byte) 0x50 &&
                    header[2] == (byte) 0x4E &&
                    header[3] == (byte) 0x47);
            return isJpeg || isPng;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed reading image header from GCS", ex);
        }
    }

    public boolean deleteByPublicUrl(String publicUrl) {
        BlobId blobId = extractBlobIdFromUrl(publicUrl);
        return storage.delete(blobId);
    }

    private BlobId extractBlobIdFromUrl(String publicUrl) {
        URI uri = URI.create(publicUrl);
        String host = uri.getHost();
        // getRawPath() preserves URL-encoded characters so we can decode cleanly
        String rawPath = uri.getRawPath();

        if (host == null || rawPath == null) {
            throw new IllegalArgumentException("Invalid GCS URL: " + publicUrl);
        }

        String bucketName;
        String objectName;

        if ("storage.googleapis.com".equals(host) || "storage.cloud.google.com".equals(host)) {
            // Format: https://://googleapis.com/my-bucket/folder/file.jpg
            String pathWithoutLeadingSlash = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
            int firstSlashIndex = pathWithoutLeadingSlash.indexOf('/');

            if (firstSlashIndex == -1) {
                throw new IllegalArgumentException("URL does not contain an object path: " + publicUrl);
            }

            bucketName = pathWithoutLeadingSlash.substring(0, firstSlashIndex);
            objectName = pathWithoutLeadingSlash.substring(firstSlashIndex + 1);

        } else if (host.endsWith(".storage.googleapis.com")) {
            // Format: https://my-bucket.://googleapis.com/folder/file.jpg
            bucketName = host.substring(0, host.length() - ".storage.googleapis.com".length());
            objectName = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;

        } else {
            throw new IllegalArgumentException("Unsupported GCS URL host: " + host);
        }

        // Decode URL-encoded characters (e.g., %20 -> space)
        String decodedObjectName = URLDecoder.decode(objectName, StandardCharsets.UTF_8);

        return BlobId.of(bucketName, decodedObjectName);
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
