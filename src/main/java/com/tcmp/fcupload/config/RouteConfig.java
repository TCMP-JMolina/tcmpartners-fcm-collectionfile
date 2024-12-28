package com.tcmp.fcupload.config;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;


import com.tcmp.fcupload.rou.FileUploadRouter;
import com.tcmp.fcupload.service.BlobService;
import com.tcmp.fcupload.service.CollectionReqService;
import com.tcmp.fcupload.service.UploadService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
@RequiredArgsConstructor
public class RouteConfig {
    private final CollectionReqService collectionReqService;
    private final BlobService blobService;
    private final UploadService uploadService;
    private final SftpConfig sftpConfig;
    private final BlobClientConfig blobClientConfig;

    @Bean(name = "fileRoute")
    public FileUploadRouter roleRoute() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return new FileUploadRouter(sftpConfig, blobClientConfig, privateKey(), collectionReqService, blobService, uploadService);
    }

    public File privateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String pem = sftpConfig.getPrivatekey().trim();
        String privateKeyPEM = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(keySpec);
        return getPEMFile(key);
    }

    public static File getPEMFile(PrivateKey privateKey) throws IOException {
        File file = new File(System.getProperty("user.home"), "privatekey.pem");

        try (FileWriter fileWriter = new FileWriter(file);
             PemWriter pemWriter = new PemWriter(fileWriter)) {
            PemObject pemObject = new PemObject("PRIVATE KEY", privateKey.getEncoded());
            pemWriter.writeObject(pemObject);
        }
        return file;
    }
}
