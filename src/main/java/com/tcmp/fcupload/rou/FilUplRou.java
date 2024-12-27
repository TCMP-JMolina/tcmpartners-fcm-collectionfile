package com.tcmp.fcupload.rou;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.tcmp.fcupload.dto.Batch;
import com.tcmp.fcupload.srv.CollectionFileService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.tcmp.fcupload.srv.UplSrv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilUplRou extends RouteBuilder {

    private final UplSrv uplSrv;

    @Value("${sftp.allowedExtensions}")
    private String allowedExtensionsString;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private String sftpPort;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.binfolder}")
    private String binfolder;

    @Value("${sftp.urlparams}")
    private String urlparams;

    @Value("${sftp.fileoutput}")
    private String fileOutputPath;

    @Value("${sftp.privatekey}")
    private String privateKeyPath;

    @Value("${sftp.knownhosts}")
    private String knownHostsFile;

    @Value("${azure.blob.containerName}")
    private String containerName;

    @Value("${azure.blob.accountName}")
    private String accountName;

    @Value("${azure.blob.sas-token}")
    private String sasToken;

    @Value("${camel.component.kafka.brokers}")
    private String kafkaBrokers;

//    @Value("${kafka.topic.batch}")
//    private String kafkaTopic;

    @Autowired
    BlobServiceClient blobServiceClient;

    private String kafkaTopic = "batch-processing-topic";
    private final CollectionFileService collectionFileService;

    @Override
    public void configure() throws Exception {

        from("azure-storage-blob://" + accountName + "/" + containerName + "?prefix=recaudos/test/&operation=listBlobs&serviceClient=#blobServiceClient")
                .routeId("blob-storage-watch")
                .split(body()) // Procesar cada blob listado
                .process(exchange -> {
                    // Obtener el nombre del blob y sus propiedades
                    String blobName = exchange.getIn().getHeader(BlobConstants.BLOB_NAME, String.class);

                    if (blobName != null && blobName.contains(".")) { // Solo procesar archivos
                        log.info("Blob listed: " + blobName);

                        BlobClient blobClient = blobServiceClient
                                .getBlobContainerClient(containerName)
                                .getBlobClient(blobName);

                        BlobProperties properties = blobClient.getProperties();
                        Map<String, String> metadata = properties.getMetadata();

                        // Adjuntar metadata y nombre del blob a los headers
                        exchange.getIn().setHeader("CamelAzureStorageBlobMetadata", metadata);
                        exchange.getIn().setHeader("originalBlobName", blobName);
                    } else {
                        log.warn("Skipping directory or non-file blob: " + blobName);
                        exchange.setProperty(Exchange.SPLIT_COMPLETE, true); // Completar si no es archivo
                    }
                })
                .filter(exchange -> exchange.getIn().getHeader("CamelAzureStorageBlobMetadata") != null)
                // Procesar solo blobs con metadata
                .process(this::processFile) // Llamar al servicio de procesamiento
                .process(exchange -> {
                    // Mover el archivo procesado a la carpeta `out`
                    String originalBlobName = exchange.getIn().getHeader("originalBlobName", String.class);
                    String destinationBlobName = originalBlobName.replace("recaudos/test/", "recaudos/out/");

                    BlobClient sourceBlobClient = blobServiceClient
                            .getBlobContainerClient(containerName)
                            .getBlobClient(originalBlobName);

                    BlobClient destinationBlobClient = blobServiceClient
                            .getBlobContainerClient(containerName)
                            .getBlobClient(destinationBlobName);

                    String sourceUrlWithSas = sourceBlobClient.getBlobUrl() + "?" + sasToken;

                    try {
                        // Copiar el archivo al destino
                        destinationBlobClient.copyFromUrl(sourceUrlWithSas);
                        log.info("File copied to: " + destinationBlobName);

                        // Eliminar el archivo original en `in`
                        sourceBlobClient.delete();
                        log.info("File deleted from source: " + originalBlobName);
                    } catch (Exception e) {
                        log.error("Error moving blob from 'in' to 'out': ", e);
                        throw e; // Lanza la excepción para manejarla en la ruta
                    }
                })
                .log("File successfully processed and moved to 'out'.")
        ;


        from("direct:processFileAndSendToKafka")
                .routeId("process-file-send-batch")
                .process(exchange -> {
                    // Obtén el objeto Batch desde el cuerpo del mensaje
                    Batch batch = exchange.getIn().getBody(Batch.class);

                    // Validación o procesamiento adicional si es necesario
                    log.info("Processing Batch: {}", batch);
                })
                .marshal().json(JsonLibrary.Jackson) // Convierte el objeto Batch a JSON
                .to("kafka:" + kafkaTopic + "?brokers=" + kafkaBrokers) // Usa las propiedades configuradas
                .log("Batch enviado a Kafka: ${body}");


        from("direct:SendPayment")
                .routeId("send-payment-to-activemq")
                .doTry()
                .log("Received body and sending to the COLLECTIONFILE_NOT: ${body}")
                .to("activemq:queue:COLLECTIONFILE_NOTIF")
                .log("Mensaje enviado a COLLECTIONFILE_NOTIF: ${body}")
                .doCatch(Exception.class)
                .log("Error al enviar el mensaje a COLLECTIONFILE_NOTIF: ${exception.message}")
                .doFinally()
                .log("Proceso de envío a COLLECTIONFILE_NOTIF finalizado.");



        from("activemq:queue:COLLECTIONFILE_REQ" )
                .log("Listening the COLLECTIONFILE_REQ: ${body}")
                .process(new CollectionReqProcessor(collectionFileService))
                .end();

        from("direct:sendTo_COLLECTIONFILE_RESP")
                .log("Sending the sucessfull to message COLLECTIONFILE_RESP: ${body}")
                .to("activemq:queue:COLLECTIONFILE_RESP");

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

    private void processFile(Exchange exchange) {
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        if (inputStream != null && isAcceptedFile(exchange)) {
            exchange.getIn().setBody(inputStream);
            uplSrv.processFile(exchange);
        } else {
            log.warn("No InputStream available in exchange.");
        }
    }


}
