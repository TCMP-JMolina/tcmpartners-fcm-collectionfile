package com.tcmp.fcupload.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class Metadata {

	private String batchId;
	private String referencia;
	private String clientName;
	private String clientId;
	private String expirationDate;
	private String customerUserId;
	private String accountNumber;
}
