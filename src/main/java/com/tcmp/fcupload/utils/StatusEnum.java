package com.tcmp.fcupload.utils;

public enum StatusEnum {

  SUCCESS("0001", "File Uploaded Successfully", "Carga Correcta"),
  PARTIAL_SUCCESS(
      "0002", "File Uploaded Successfully with Rejected Records", "Carga Parcialmente Correcta"),
  FAILED("0003", "Uploaded Failed", "Carga Fallida");

  private final String code;
  private final String fcmMessage;
  private final String description;

  StatusEnum(String code, String fcmMessage, String description) {
    this.code = code;
    this.fcmMessage = fcmMessage;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getFcmMessage() {
    return fcmMessage;
  }

  public String getDescription() {
    return description;
  }

}