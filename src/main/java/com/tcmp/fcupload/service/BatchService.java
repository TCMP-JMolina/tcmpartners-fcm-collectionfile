package com.tcmp.fcupload.service;

import com.tcmp.fcupload.mdl.DtlCompany;
import com.tcmp.fcupload.rep.DtlCompanyRep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

	private final DtlCompanyRep dtlCompanyRep; // Inyección del repository

	/**
	 * Método para obtener el shortName basado en el clientId.
	 * @param clientId Identificador del cliente.
	 * @return El shortName del cliente si existe, o un mensaje de advertencia si no.
	 */
	public String getShortNameByClientId(String clientId) {
		Optional<DtlCompany> companyOpt = dtlCompanyRep.findByClientId(clientId).stream().findFirst();

		return companyOpt.map(DtlCompany::getShortName) // Obtén el shortName si existe
				.orElseGet(() -> {
					log.warn("No company found for clientId: {}", clientId);
					return "No short name found for clientId: " + clientId;
				});
	}

	/**
	 * Consulta el clientType basado en el cif.
	 *
	 * @param cif El cif a consultar.
	 * @return El clientType si se encuentra, de lo contrario, un mensaje de error.
	 */
	public String getClientTypeByCif(String cif) {
		Optional<DtlCompany> companyOpt = dtlCompanyRep.findByClientId(cif).stream().findFirst();

		return companyOpt.map(DtlCompany::getClientType)
				.orElseGet(() -> {
					log.warn("No company found for cif: {}", cif);
					return "No client type found for cif: " + cif;
				});
	}

	public String getClientName(String clientId) {
		Optional<DtlCompany> companyOpt = dtlCompanyRep.findByClientId(clientId).stream().findFirst();

		return companyOpt.map(DtlCompany::getClientName)
				.orElseGet(() -> {
					log.warn("No company found for clientId: {}", clientId);
					return "No client name found for clientId: " + clientId;
				});
	}

}
