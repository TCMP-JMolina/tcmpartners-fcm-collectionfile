package com.tcmp.fcupload.rou;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.tcmp.fcupload.service.BlobService;
import com.tcmp.fcupload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.azure.storage.blob.BlobConstants;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class BlobProcessor implements Processor {

    private final BlobService blobService;

    @Override
    public void process(Exchange exchange) {
        log.info("Starting process method");

        try {
            List<BlobItem> blobsToProcess = blobService.getBlobsForProcessing();
            blobService.processListOfBlobs(blobsToProcess, exchange);


        } catch (Exception e) {
            log.error("Error processing blobs: {}", e.getMessage(), e);
        }

        log.info("Process method completed");
    }






}
