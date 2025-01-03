package com.tcmp.fcupload.config;

import lombok.Data;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "camel")
public class BlobClientConfig {
    private String user;
    private String password;
    private Component component;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint(component.getAzureStorageBlob().getEndpoint())
                .sasToken(component.getAzureStorageBlob().getSasToken())
                .buildClient();
    }

    @Data
    public static class Component {
        private AzureStorageBlob azureStorageBlob;

        @Data
        public static class AzureStorageBlob {
            private String sasToken;
            private String endpoint;
            private String accountName;
            private String containerName;
            private String allowedExtensions;
            private String readDirectory;
            private String copyDirectory;
        }
    }
}
