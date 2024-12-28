package com.tcmp.fcupload.service;

import com.tcmp.fcupload.dto.Instruments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InstrumentService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public List<Instruments> createInstrument (Map<String, Object> metadataFile, List<Map<String, String>> fileLinesReaded ){

		List<Instruments> instruments = new ArrayList<>();

    for (int i = 0; i < fileLinesReaded.size(); i++) {

			Instruments instrument = new Instruments();

			instrument.setId(metadataFile.get("batchid").toString());
			instrument.setItemId(Integer.toString(i+1));
			instrument.setBatchId(metadataFile.get("batchid").toString());
			instrument.setInstrumentId(Integer.toString(i+1));
			instrument.setUserData(null);
			instrument.setProcessingData(null);
			instrument.setStatus(fileLinesReaded.get(i).get("status"));
			instrument.setCreateDate(LocalDate.now().toString());
			instrument.setLastDate("2024-12-28"); // fecha ultima actualizacion
			//instrument.setPaymentMethod(getPaymentMethod(metadataFile.get("Subserviceid").toString())); //obtener tabla BD columna productDescription tabla MV_PRODUCT_MST busco primero codigo y luego la descripcion
			instrument.setPaymentMethod("example");
		float amount = 	Float.parseFloat(fileLinesReaded.get(i).get("amount"));
			instrument.setAmount(amount);
			instrument.setCurrency("USD"); //obtener BD **
			instrument.setCounterparty(fileLinesReaded.get(i).get("clientName"));
			instrument.setCounterpartyId("1234567890"); //numero cc o ruc
			instrument.setCounterpartyIdType("CC"); // tipo identificacion enum tipos identificacion
			instrument.setCounterpartyAccountType("SAVINGS"); // consultar en BD preguntar a frank**
			instrument.setCounterpartyAccount(fileLinesReaded.get(i).get("accountNumber")); //numero de cuenta
			instrument.setCounterpartyAccountCountry("EC"); // EC puede ser quemado o consultado
			instrument.setCounterpartyAccountBankId("0010" ); //obtener BD preguntar a frank ***
			instrument.setCounterpartyAccountBankName("BANK NAME"); // obtener BD nombre banco
			instrument.setClientId("CLIENTID-"); // ruc de empresa  alternatename
			instrument.setCustomerAffiliationId(metadataFile.get("cif").toString());
			instrument.setClientName("CLIENT NAME-"); //nombre de la empresa
			instrument.setCustomerUserId(metadataFile.get("uploaduser").toString());
			instrument.setTypeId(metadataFile.get("subserviceid").toString());
			instrument.setType(metadataFile.get("producttype").toString());
			instrument.setCustomerReference(metadataFile.get("reference").toString());
			instrument.setAdditionalReference(null);

			instruments.add(instrument);
		}

		log.info("Successfully iterated instruments.");
		return instruments;
	}

	public String getPaymentMethod(String productCode) {
		try {
			String sql = "SELECT PRODUCT_DESCRIPTION FROM MV_PRODUCT_MST WHERE PRODUCT_CODE = ?";
			return jdbcTemplate.queryForObject(sql, new Object[]{productCode}, String.class);
		} catch (Exception e) {
			throw new RuntimeException("Error fetching product description for productCode: " + productCode, e);
		}
	}



}
