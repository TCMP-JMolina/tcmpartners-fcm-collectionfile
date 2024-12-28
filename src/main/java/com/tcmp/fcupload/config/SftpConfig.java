package com.tcmp.fcupload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpConfig {
  private String user;
  private String privatekey;
  private String host;
  private String port;
  private Root root;

  @Data
  public static class Root {
    private String readDirectory;
    private String localDirectory;
    private String destinationDir;
  }

}
