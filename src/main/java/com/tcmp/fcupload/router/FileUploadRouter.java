package com.tcmp.fcupload.router;


import com.azure.storage.blob.BlobServiceClient;
import com.tcmp.fcupload.config.BlobClientConfig;
import com.tcmp.fcupload.service.BlobService;
import com.tcmp.fcupload.service.CollectionReqService;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadRouter extends RouteBuilder {



    private final BlobClientConfig blobClientconfig;
    private final CollectionReqService reqService;
    private final BlobService blobService;
    private final BlobServiceClient blobServiceClient;
    private final BlobClientConfig blobClientConfig;


    @Value("${camel.component.kafka.brokers}")
    private String kafkaBrokers;

//    @Value("${kafka.topic.batch}")
//    private String kafkaTopic;

    private String kafkaTopic = "batch-processing-topic";


    @Override
    public void configure() throws Exception {

        from("azure-storage-blob://" + blobClientconfig.getComponent().getAzureStorageBlob().getAccountName() +
                "/" + blobClientconfig.getComponent().getAzureStorageBlob().getContainerName() +
                "?prefix=" + blobClientconfig.getComponent().getAzureStorageBlob().getReadDirectory() +
                "&operation=listBlobs&serviceClient=#blobServiceClient" +
                "&delay=60000")
                .routeId("blob-storage-watch")
                .split(body())
                .process(new BlobProcessor(blobService, blobServiceClient, blobClientConfig))
                .log("The process is finished");


//        from("direct:processFileAndSendToKafka")
//                .routeId("process-file-send-batch")
//                .process(exchange -> {
//                    Batch batch = exchange.getIn().getBody(Batch.class);
//                    log.info("Processing Batch: {}", batch);
//                })
//                .marshal().json(JsonLibrary.Jackson)
//                .to("kafka:" + kafkaTopic + "?brokers=" + kafkaBrokers)
//                .log("Batch sent to Kafka: ${body}");


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


        from("activemq:queue:COLLECTIONFILE_REQ")
                .choice()
                .when(xpath("//*[local-name()='FndtMsg']/*[local-name()='Header']/*[local-name()='eventCode' and text()='PAY_TXN_HANDOFF']"))
                .log("Listening the COLLECTIONFILE_REQ: ${body}")
                .process(new CollectionReqProcessor(reqService))
                .otherwise();

        from("direct:sendResponseFCM")
                .log("Sending the sucessfull to message COLLECTIONFILE_RESP: ${body}")
                .to("activemq:queue:COLLECTIONFILE_RESP");
    }
}
