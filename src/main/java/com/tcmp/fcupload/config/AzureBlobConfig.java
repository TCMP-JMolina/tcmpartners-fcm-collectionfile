package com.tcmp.fcupload.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

	@Value("${azure.blob.endpoint}")
	private String blobEndpoint;

	@Value("${azure.blob.sas-token}")
	private String sasToken;

	@Bean
	public BlobServiceClient blobServiceClient() {
		return new BlobServiceClientBuilder()
				.endpoint(blobEndpoint)
				.sasToken(sasToken)
				.buildClient();
	}
}
