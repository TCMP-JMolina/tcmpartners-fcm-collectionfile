package com.tcmp.fcupload.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.tcmp.fcupload.config.BlobClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlobService {


    private final BlobClientConfig blobClientConfig;
    private final UploadService uploadService;


    public void processFileMetadata(BlobClient blobClient, String fileName, Exchange exchange) throws IOException {
        BlobProperties properties = blobClient.getProperties();
        Map<String, String> metadata = properties.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            exchange.getIn().setHeader("CamelAzureStorageBlobMetadata", metadata);
            exchange.getIn().setHeader("originalBlobName", fileName);
        } else {
            log.warn("No metadata found for blob: {}", blobClient.getBlobName());
        }
    }


    public void processFileContent( String fileName, Exchange exchange) throws IOException {

        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        if (inputStream != null && isAcceptedFile(fileName)) {
            exchange.getIn().setBody(inputStream);
            Map<String, Object> blobMetadataRaw = exchange.getIn().getHeader("CamelAzureStorageBlobMetadata", Map.class);
            Long fileSize = exchange.getIn().getHeader(BlobConstants.BLOB_SIZE, Long.class);
            uploadService.processFile(exchange, blobMetadataRaw, fileName, fileSize);
        } else {
            log.warn("No InputStream available in exchange.");
        }


    }

    private boolean isAcceptedFile(String fileName) {

        List<String> allowedExtensions = Arrays.asList(blobClientConfig.getComponent().getAzureStorageBlob().getAllowedExtensions().split(","));
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if (allowedExtensions.contains(extension)) {
            log.info("Accepted file format: {}", extension);
            return true;
        } else {
            log.warn("Rejected file format: {}", extension);
            return false;
        }
    }

    //    private void moveBlob(Exchange exchange) {
//        String originalBlobName = exchange.getIn().getHeader("originalBlobName", String.class);
//        String destinationBlobName = originalBlobName.replace("recaudos/test/", "recaudos/out/");
//
//        azureBlobService.copyBlob(originalBlobName, destinationBlobName);
//
//        log.info("File successfully processed and moved to 'out'.");
//    }


}
