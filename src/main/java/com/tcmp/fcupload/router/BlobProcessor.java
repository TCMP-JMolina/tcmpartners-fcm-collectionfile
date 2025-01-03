package com.tcmp.fcupload.router;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.tcmp.fcupload.config.BlobClientConfig;
import com.tcmp.fcupload.service.BlobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class BlobProcessor implements Processor {

    private final BlobService blobService;
    private final BlobServiceClient blobServiceClient;
    private final BlobClientConfig blobClientConfig;

    @Override
    public void process(Exchange exchange) throws Exception {
        String containerName = "sftp01";
        String blobName = exchange.getIn().getHeader(BlobConstants.BLOB_NAME, String.class);

        if (blobName != null && blobName.contains(".")) {
            log.info("Listening blob: {}", blobName);

            BlobClient blobClient = blobServiceClient
                    .getBlobContainerClient(containerName)
                    .getBlobClient(blobName);

            blobService.processFileMetadata(blobClient, blobName, exchange);
            blobService.processFileContent( blobName, exchange);
            blobService.moveBlobHeader(exchange, containerName);


        } else {
            log.warn("Skipping directory or non-file blob: {}", blobName);
            exchange.setProperty(Exchange.SPLIT_COMPLETE, true);
        }
    }


}
