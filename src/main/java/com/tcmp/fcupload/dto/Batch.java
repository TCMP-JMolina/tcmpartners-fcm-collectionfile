package com.tcmp.fcupload.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch implements Serializable {

	private String batchId;
	private String referencia;
	private String clientName;
	private String alternateName;
	private String clientId;
	private String type;
	private String typeId;
	private String createDate;
	private String startDate;
	private String endDate;
	private String executionDate;
	private String expirationDate;
	private String lastDate;
	private String medio;
	private String checksum;
	private String customerAffiliationId;
	private String customerUserId;
	//	private List< approversList > approvers = new ArrayList< >();
	private String customerProductId;
	private String fileName;
	private String inputFile;
	private String outputFile;
	private String idStatus;
	private String status;
	private Integer totalItems;
	private Integer totalItemsRejected;
	private Integer totalItemsUploaded;
	private String accountNumber;
	private String accountType;
	private Float totalAmmount;

	// Clase interna
	@Data
	@AllArgsConstructor
	@Builder
	public static class approversList implements Serializable {
		private String checkerUser;
		private String approveDate;
		private String status;
		private String rejectRemarks;
	}
}


