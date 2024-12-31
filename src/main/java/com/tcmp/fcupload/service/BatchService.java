package com.tcmp.fcupload.service;

import com.tcmp.fcupload.model.ClientSetUp;
import com.tcmp.fcupload.repository.ClientSetUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

	private final ClientSetUpRepository clientSetUpRepository;


	public String getShortNameByClientId(String clientId) {
		Optional<ClientSetUp> clientSetUp = clientSetUpRepository.findByClientId(clientId).stream().findFirst();

		return clientSetUp.map(ClientSetUp::getShortName) //
				.orElseGet(() -> {
					log.warn("No shortname found for clientId in clientSetUpRep: {}", clientId);
					return "No short name found for clientId: " + clientId;
				});
	}

	public String getClientTypeByCif(String cif) {
		Optional<ClientSetUp> clientSetUp = clientSetUpRepository.findByClientId(cif).stream().findFirst();

		return clientSetUp.map(ClientSetUp::getClientType)
				.orElseGet(() -> {
					log.warn("No client type found for cif in clientSetUpRep: {}", cif);
					return "No client type found for cif: " + cif;
				});
	}

	public String getClientName(String clientId) {
		Optional<ClientSetUp> clientSetUp = clientSetUpRepository.findByClientId(clientId).stream().findFirst();

		return clientSetUp.map(ClientSetUp::getClientName)
				.orElseGet(() -> {
					log.warn("No name found for clientId in clientSetUpRep: {}", clientId);
					return "No client name found for clientId: " + clientId;
				});
	}

}
