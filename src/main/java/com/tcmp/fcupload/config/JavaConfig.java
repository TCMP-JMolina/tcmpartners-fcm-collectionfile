package com.tcmp.fcupload.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JavaConfig {

	@Value("${java.version}")
	private String javaVersion;

	public void printJavaVersion() {
		System.out.println("Java Version: " + javaVersion);
	}
}