package com.tcmp.fcupload.utils;

import com.tcmp.fcupload.dto.Batch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BatchBuilder {

//	private final ExternalServiceClient externalServiceClient; // Cliente para llamadas externas
//
//	public BatchBuilder(ExternalServiceClient externalServiceClient) {
//		this.externalServiceClient = externalServiceClient;
//	}

	public Batch buildBatch(File file) {
		// Extrae valores básicos de la metadata del archivo
		String fileName = file.getName();
		String fileUploadId = generateUploadId(fileName);
		String checksum = calculateChecksum(file);
		int totalItems = countFileRows(file);
		Date currentDate = new Date();

		// Llamadas a servicios externos
//		String clientName = externalServiceClient.getClientName(fileUploadId);
//		String alternateName = externalServiceClient.getAlternateName(fileUploadId);
//		String endDate = externalServiceClient.getEndDate(fileUploadId);
//		String lastDate = externalServiceClient.getLastDate(fileUploadId);
//		String accountType = externalServiceClient.getAccountType(fileUploadId);
//		Float totalAmount = externalServiceClient.getTotalAmount(fileUploadId);
//		String productId = externalServiceClient.getProductId(fileUploadId);
//		String productName = externalServiceClient.getProductName(productId);

		// Rellena la lista de aprobadores con datos por defecto
		List<Batch.approversList> approvers = new ArrayList<>();

		return Batch.builder()
				.batchId(fileUploadId)
				.referencia(generateReferencia(fileUploadId))
//				.clientName(clientName)
//				.alternateName(alternateName)
//				.clientId(fileUploadId)
//				.type(productName)
//				.typeId(productId)
//				.createDate(formatDate(currentDate))
//				.startDate(formatDate(currentDate))
//				.endDate(endDate)
//				.executionDate(formatDate(currentDate))
//				.expirationDate("2024-12-31") // Placeholder
//				.lastDate(lastDate)
//				.medio("SFTP")
//				.checksum(checksum)
//				.customerAffiliationId("ClientCode123") // Placeholder
//				.customerUserId("User123") // Placeholder de metadata
//				.approvers(approvers)
//				.customerProductId(productId)
//				.fileName(fileName)
//				.inputFile(null)
//				.outputFile(null)
//				.idStatus("001") // Placeholder
//				.status("Loaded") // Placeholder
//				.totalItems(totalItems)
//				.totalItemsRejected(0) // Placeholder, ajustar en tiempo real
//				.totalItemsUploaded(totalItems) // Placeholder, ajustar en tiempo real
//				.accountNumber("Account123") // Placeholder, llenar de metadata
//				.accountType(accountType)
//				.totalAmmount(totalAmount)
				.build();
	}

	private String generateUploadId(String fileName) {
		return fileName.replace(".", "_").toUpperCase();
	}

	private String calculateChecksum(File file) {
		// Implementación real para calcular checksum
		return "checksum123"; // Placeholder
	}

	private int countFileRows(File file) {
		// Implementación real para contar filas
		return 100; // Placeholder
	}

	private String formatDate(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}

	private String generateReferencia(String fileUploadId) {
		return "REF_" + fileUploadId;
	}
}
