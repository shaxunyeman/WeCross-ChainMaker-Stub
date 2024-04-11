package com.webank.wecross.stub.chainmaker.account;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.config.AuthType;
import org.chainmaker.sdk.utils.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerAccountFactory {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountFactory.class);

  private final String stubType;

  private ChainMakerAccountFactory(String stubType) {
    this.stubType = stubType;
  }

  public static ChainMakerAccountFactory getInstance(String stubType) {
    return new ChainMakerAccountFactory(stubType);
  }

  public ChainMakerPublicAccount build(Map<String, Object> properties) {
    String username = (String) properties.get("username");
    Integer keyID = (Integer) properties.get("keyID");
    String type = (String) properties.get("type");
    Boolean isDefault = (Boolean) properties.get("isDefault");
    String secKey = (String) properties.get("secKey");
    String pubKey = (String) properties.get("pubKey");
    String address = (String) properties.get("ext0");

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

    if (address == null || address.isEmpty()) {
      logger.error("address has not given in ext0");
      return null;
    }

    try {
      logger.info("New account: {} type:{}", username, type);

      PrivateKey privateKey =
          CryptoUtils.getPrivateKeyFromBytes(secKey.getBytes(StandardCharsets.UTF_8));
      PublicKey publicKey = CryptoUtils.getPublicKeyFromPrivateKey(privateKey);
      // TODO no orgId ?
      User user = new User("");
      user.setAuthType(AuthType.Public.getMsg());
      user.setPrivateKey(privateKey);
      user.setPublicKey(publicKey);
      ChainMakerPublicAccount account = new ChainMakerPublicAccount(username, type, user);

      if (!account.getIdentity().equals(address)) {
        throw new Exception("Given address is not belongs to the secKey of " + username);
      }

      return account;
    } catch (Exception e) {
      logger.error("ChainMakerPublicAccount exception: {}", e.getMessage());
      return null;
    }
  }
}
