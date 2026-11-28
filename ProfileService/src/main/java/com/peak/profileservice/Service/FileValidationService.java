package com.peak.profileservice.Service;

import java.io.IOException;

public interface FileValidationService {

    public void validateAndPromoteFile(String messageId, String blobName, String userId) throws IOException;
}
