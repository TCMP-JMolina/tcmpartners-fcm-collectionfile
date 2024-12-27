package com.tcmp.fcupload.srv.FileTypes;

import java.util.List;

public interface FileProcessor {
    void process(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                 String uploadUser, String subserviceId, String subservice, String accountNumber);
}
