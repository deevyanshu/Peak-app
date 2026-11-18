package com.peak.filevalidationservice.Controller;

import com.google.events.cloud.storage.v1.StorageObjectData;
import com.peak.filevalidationservice.ExceptionHandler.BadRequestException;
import com.peak.filevalidationservice.Service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FileValidationController {

    private final FileValidationService fileValidationService;

    @PostMapping("/")
    public ResponseEntity<Void> validateFile(@RequestBody StorageObjectData eventData)
    {
        String blobName= eventData.getName();

        if(blobName==null || !blobName.startsWith("pending/"))
        {
            throw new BadRequestException("Invalid blob name. Expected to start with 'pending/'.", HttpStatus.BAD_REQUEST);
        }

        String userId = extractUserIdFromPath(blobName);

        // 3. Trigger your validation service
        fileValidationService.validateAndPromoteFile(blobName, userId);
        return ResponseEntity.ok(null);
    }

    private String extractUserIdFromPath(String blobName) {
        // Splits "pending/user_123/avatar.jpg" into segments
        String[] parts = blobName.split("/");
        return parts[1]; // Returns "user_123"
    }
}
