package com.tcmp.fcupload.router;

import com.tcmp.fcupload.service.CollectionReqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CollectionReqProcessor implements Processor {

    private final CollectionReqService collectionReqService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        collectionReqService.processCollectionFileRequest(body);
    }
}
