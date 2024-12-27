package com.tcmp.fcupload.mdl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "CLIENT_SERVICE_SETUP", schema = "FCM")
public class DtlCompany {

	@Id
	@Column(name = "CLIENT_ID", length = 12, nullable = false)
	private String clientId;

	@Column(name = "CLIENT_NAME", length = 255)
	private String clientName;

	@Column(name = "CLIENT_SHORT_NAME", length = 255)
	private String shortName;

	@Column(name = "CLIENT_TYPE", length = 1, nullable = false, columnDefinition = "DEFAULT 'M'")
	private String clientType;

	public void setClientName(String testCompany) {
	}

	public void setShortName(String tsc) {

	}
}
