package com.tcmp.fcupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instruments {
	private String id;
	private String itemId;
	private String batchId;
	private String instrumentId;
	private String userData; // Asumiendo que JSONB es un String que luego puedes procesar
	private String processingData; // Opcional, lo puedes dejar como null si no está presente
	private String status;
	private String createDate;
	private String lastDate;
	private String paymentMethod;
	private float amount;
	private String currency;
	private String counterparty;
	private String counterpartyId;
	private String counterpartyIdType;
	private String counterpartyAccountType;
	private String counterpartyAccount;
	private String counterpartyAccountCountry;
	private String counterpartyAccountBankId;
	private String counterpartyAccountBankName;
	private String clientId;
	private String customerAffiliationId;
	private String clientName;

	 private String customerUserId; //viene heredado de la orden
	 private String typeId; //viene heredado de la orden
	 private String type; //viene heredado de la orden
	 private String customerReference;
	 private String additionalReference; // id de contrapartida
}