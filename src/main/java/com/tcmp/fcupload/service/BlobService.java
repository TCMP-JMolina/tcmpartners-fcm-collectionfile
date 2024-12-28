package com.tcmp.fcupload.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlobService {


    private final BlobClientConfig blobClientConfig;
    private final FcmConfig fcmConfig;
    private final SftpConfig sftpConfig;
    private final FileService fileService;

    private BlobContainerClient containerClient;
    @Autowired
    BlobServiceClient blobServiceClient;


    @PostConstruct
    public void init() {
        String containerName = "sftp01";
        this.containerClient = blobClientConfig.blobServiceClient().getBlobContainerClient(containerName);
    }

    public List<BlobItem> getBlobsForProcessing() {

        int batchSize = Integer.parseInt(fcmConfig.getBlob().getSize());

        // Collect the list of blobs from the container with a specific prefix
        List<BlobItem> blobs = new ArrayList<>();
        containerClient.listBlobs(new ListBlobsOptions().setPrefix(sftpConfig.getRoot().getReadDirectory()), null).forEach(blobs::add);

        // Define the directory
        String userHome = System.getProperty("user.home");
        File subFolder = new File(userHome, sftpConfig.getRoot().getLocalDirectory());
        if (!subFolder.exists()) {
            boolean created = subFolder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create the subfolder: " + subFolder.getAbsolutePath());
            }
        }

        // Count existing files in the local directory
        File[] existingFiles = subFolder.listFiles();
        int existingFileCount = (existingFiles != null) ? existingFiles.length : 0;

        // Calculate how many files are needed to reach the batch size
        int filesToDownload = Math.max(0, batchSize - existingFileCount);

        // Determine the range of blobs to process (limited by filesToDownload and blob count)
        int end = Math.min(filesToDownload, blobs.size());

        // Return the sublist of blobs to be processed
        return blobs.subList(0, end);
    }

    public void processListOfBlobs(List<BlobItem> blobsToProcess, Exchange exchange) {
        if (blobsToProcess != null && !blobsToProcess.isEmpty()) {
            for (BlobItem blobItem : blobsToProcess) {
                BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                processBlob(blobClient, blobItem, exchange);

            }
        } else {
            log.info("No blobs found to process.");
        }
    }

    public void processBlob(BlobClient blobClient, BlobItem blobItem, Exchange exchange) {
        try {
            long blobSize = blobClient.getProperties().getBlobSize();
            if (blobSize > 0) {

                int lastSlashIndex = blobItem.getName().lastIndexOf('/');
                String fileName = blobItem.getName().substring(lastSlashIndex + 1);

                if (fileName.contains(".")) {
                    log.info("Going to process this file blob: {}", fileName);
                    processFileMetadata(blobClient, fileName, exchange);
                    processFileContent(exchange);

                } else {
                    log.warn("Skipping directory or non-file blob: " + fileName);
                }

            } else {
                log.warn("Blob size is 0. Skipping blob: {}", blobItem.getName());
            }

        } catch (Exception e) {
            log.error("Error processing blob {}: {}", blobItem.getName(), e.getMessage());
        }
    }

    private void processFileMetadata(BlobClient blobClient, String fileName, Exchange exchange) throws IOException {
        BlobProperties properties = blobClient.getProperties();
        Map<String, String> metadata = properties.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            exchange.getIn().setHeader("CamelAzureStorageBlobMetadata", metadata);
            exchange.getIn().setHeader("originalBlobName", fileName);
        } else {
            log.warn("No metadata found for blob: {}", blobClient.getBlobName());
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

    private void processFileContent(Exchange exchange) {

        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        if (inputStream != null && isAcceptedFile(exchange)) {
            uploadService.processFile(inputStream);
        } else {
            log.warn("No InputStream available in exchange.");
        }
    }

    private boolean isAcceptedFile(Exchange exchange) {

        String fileName = exchange.getIn().getHeader(BlobConstants.BLOB_NAME, String.class);

        List<String> allowedExtensions = Arrays.asList(allowedExtensionsString.split(","));

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if (allowedExtensions.contains(extension)) {
            log.info("Accepted file format: " + extension);
            return true;
        } else {
            log.warn("Rejected file format: " + extension);
            return false;
        }
    }


}
