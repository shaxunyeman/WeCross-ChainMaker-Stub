package com.webank.wecross.stub.chainmaker.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import com.webank.wecross.stub.chainmaker.config.ChainMakerAccountConfig;
import com.webank.wecross.stub.chainmaker.config.ChainMakerAccountConfigParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.chainmaker.sdk.config.AuthType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ChainMakerAccountFactory {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountFactory.class);

  private final String stubType;

  private ChainMakerAccountFactory(String stubType) {
    this.stubType = stubType;
  }

  public static ChainMakerAccountFactory getInstance(String stubType) {
    return new ChainMakerAccountFactory(stubType);
  }

  private ChainMakerWithCertAccount buildWithCertAccount(
      String stubType,
      String username,
      String ordId,
      byte[] signKeyBytes,
      byte[] signCertBytes,
      byte[] tlsKeyBytes,
      byte[] tlsCertBytes,
      boolean pkcs11Enable)
      throws Exception {
    ChainMakerWithCertAccount account =
        new ChainMakerWithCertAccount(
            username,
            stubType,
            ChainMakerUserFactory.buildUserFromPrivateKeyBytes(
                signKeyBytes, signCertBytes, tlsKeyBytes, tlsCertBytes, pkcs11Enable));
    account.getUser().setOrgId(ordId);
    account.setTlsKey(tlsKeyBytes);
    account.setTlsCert(tlsCertBytes);
    return account;
  }

  private ChainMakerPublicAccount buildPublicAccount(
      String stubType, String username, byte[] secKeyBytes) throws Exception {
    ChainMakerPublicAccount account =
        new ChainMakerPublicAccount(
            username, stubType, ChainMakerUserFactory.buildUserFromPrivateKeyBytes(secKeyBytes));
    return account;
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
            buildWithCertAccount(
                type,
                username,
                ext0Map.get("orgId"),
                ext0Map.get("userSignKey").getBytes(StandardCharsets.UTF_8),
                ext0Map.get("userSignCert").getBytes(StandardCharsets.UTF_8),
                secKey.getBytes(StandardCharsets.UTF_8),
                pubKey.getBytes(StandardCharsets.UTF_8),
                ext0Map.get("pkcs11Enable").equalsIgnoreCase("true") ? true : false);
      } else {
        byte[] privateKeyBytes = secKey.getBytes(StandardCharsets.UTF_8);
        account = buildPublicAccount(type, username, privateKeyBytes);
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

  public List<ChainMakerAccount> build(String authType, String accountPath) throws Exception {
    String accountConfigFile = accountPath + File.separator + "accounts.toml";
    logger.debug("Loading account.toml: {}", accountConfigFile);

    ChainMakerAccountConfigParser parser = new ChainMakerAccountConfigParser(accountConfigFile);
    ChainMakerAccountConfig chainMakerAccountConfig = parser.loadConfig();
    List<ChainMakerAccountConfig.Account> accounts = chainMakerAccountConfig.getAccounts();
    List<ChainMakerAccount> chainMakerAccounts = new ArrayList<>();
    for (ChainMakerAccountConfig.Account account : accounts) {
      String stubType = account.getType();
      String userName = account.getName();
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      if (authType.equals(AuthType.PermissionedWithCert.getMsg())) {
        String orgId = account.getOrgId();
        Resource signKeyFileResource =
            resolver.getResource(accountPath + File.separator + account.getSignKeyPath());
        Resource signCertFileResource =
            resolver.getResource(accountPath + File.separator + account.getSignCertPath());
        Resource tlsKeyFileResource =
            resolver.getResource(accountPath + File.separator + account.getTlsKeyPath());
        Resource tlsCertFileResource =
            resolver.getResource(accountPath + File.separator + account.getTlsCertPath());
        chainMakerAccounts.add(
            buildWithCertAccount(
                stubType,
                userName,
                orgId,
                Files.toByteArray(signKeyFileResource.getFile()),
                Files.toByteArray(signCertFileResource.getFile()),
                Files.toByteArray(tlsKeyFileResource.getFile()),
                Files.toByteArray(tlsCertFileResource.getFile()),
                false));
      } else {
        Resource signKeyFileResource =
            resolver.getResource(accountPath + File.separator + account.getSignKeyPath());
        chainMakerAccounts.add(
            buildPublicAccount(
                stubType, userName, Files.toByteArray(signKeyFileResource.getFile())));
      }
    }
    return chainMakerAccounts;
  }
}
