package com.tcmp.fcupload.mdl;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NotificationFcmTypeNameEnum {
	CARGA_CORRECTA("0001", "File Uploaded Successfully"),
	PARCIALMENTE_CORRECTA("0002", "File Uploaded Successfully with Rejected Records"),
	CARGA_INCORRECTA("0003", "Uploaded Failed"),
	PROCESO_PENDIENTE_AUTORIZACION("0004", "Pending Auth"),
	APROBACION_REJECT("0005", "Reject"),
	PROCESO_SEND_BANK("0006", "Send To Bank");

	private final String id;
	private final String descripcion;

	@Override
	public String toString() {
		return "ID: " + id + ", Descripción: " + descripcion;
	}
}