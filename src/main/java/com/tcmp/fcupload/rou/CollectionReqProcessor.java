package com.tcmp.fcupload.rou;

import com.tcmp.fcupload.srv.CollectionFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CollectionReqProcessor implements Processor {

    private final CollectionFileService collectionFileService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        collectionFileService.processCollectionFileRequest(body);
    }
}
