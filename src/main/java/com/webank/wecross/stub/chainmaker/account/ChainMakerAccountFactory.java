package com.webank.wecross.stub.chainmaker.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import com.webank.wecross.stub.chainmaker.config.ChainMakerAccountConfig;
import com.webank.wecross.stub.chainmaker.config.ChainMakerAccountConfigParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.chainmaker.sdk.config.AuthType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ChainMakerAccountFactory {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountFactory.class);

  private final String stubType;

  private ChainMakerAccountFactory(String stubType) {
    this.stubType = stubType;
  }

  public static ChainMakerAccountFactory getInstance(String stubType) {
    return new ChainMakerAccountFactory(stubType);
  }

  public ChainMakerAccount build(Map<String, Object> properties) {
    String authType = (String) properties.get(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE);
    String username = (String) properties.get("username");
    Integer keyID = (Integer) properties.get("keyID");
    String type = (String) properties.get("type");
    Boolean isDefault = (Boolean) properties.get("isDefault");
    String secKey = (String) properties.get("secKey");
    String pubKey = (String) properties.get("pubKey");
    String ext0 = (String) properties.get("ext0");

    if (username == null || username.isEmpty()) {
      logger.error("username has not given");
      return null;
    }

    if (keyID == null) {
      logger.error("keyID has not given");
      return null;
    }

    if (!stubType.equals(type)) {
      logger.error("Invalid account type: {}", type);
      return null;
    }

    if (isDefault == null) {
      logger.error("isDefault has not given");
      return null;
    }

    if (secKey == null || secKey.isEmpty()) {
      logger.error("secKey has not given");
      return null;
    }

    if (pubKey == null || pubKey.isEmpty()) {
      logger.error("pubKey has not given");
      return null;
    }

    if (ext0 == null || ext0.isEmpty()) {
      logger.error("ext0 has not given");
      return null;
    }

    ChainMakerAccount account = null;
    try {
      logger.info("New account: {} type:{}", username, type);
      if (authType.equals(AuthType.PermissionedWithCert.getMsg())) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> ext0Map =
            objectMapper.readValue(ext0, new TypeReference<Map<String, String>>() {});
        account =
            new ChainMakerWithCertAccount(
                username,
                type,
                ChainMakerUserFactory.buildUserFromPrivateKeyBytes(
                    ext0Map.get("userSignKey").getBytes(),
                    ext0Map.get("userSignCert").getBytes(),
                    secKey.getBytes(),
                    pubKey.getBytes(),
                    ext0Map.get("pkcs11Enable").equalsIgnoreCase("true") ? true : false));
        account.getUser().setOrgId(ext0Map.get("orgId"));
      } else {
        byte[] privateKeyBytes = secKey.getBytes(StandardCharsets.UTF_8);
        account =
            new ChainMakerPublicAccount(
                username,
                type,
                ChainMakerUserFactory.buildUserFromPrivateKeyBytes(privateKeyBytes));
        if (!account.getIdentity().equals(ext0)) {
          throw new Exception("Given address is not belongs to the secKey of " + username);
        }
      }
      return account;
    } catch (Exception e) {
      logger.error("ChainMakerPublicAccount exception: {}", e.getMessage());
      return null;
    }
  }

  public ChainMakerPublicAccount build(String name, String accountPath) throws Exception {
    String accountConfigFile = accountPath + File.separator + "account.toml";
    logger.debug("Loading account.toml: {}", accountConfigFile);

    ChainMakerAccountConfigParser parser = new ChainMakerAccountConfigParser(accountConfigFile);
    ChainMakerAccountConfig chainMakerAccountConfig = parser.loadConfig();
    String type = chainMakerAccountConfig.getAccount().getType();
    String accountFileName = chainMakerAccountConfig.getAccount().getAccountFile();
    String accountFilePath = accountPath + File.separator + accountFileName;
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource accountFileResource = resolver.getResource(accountFilePath);
    byte[] privateKeyBytes = Files.toByteArray(accountFileResource.getFile());
    return new ChainMakerPublicAccount(
        name, type, ChainMakerUserFactory.buildUserFromPrivateKeyBytes(privateKeyBytes));
  }
}
