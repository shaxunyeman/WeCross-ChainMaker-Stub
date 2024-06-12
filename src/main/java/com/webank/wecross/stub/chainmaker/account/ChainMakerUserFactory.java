package com.webank.wecross.stub.chainmaker.account;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.config.AuthType;
import org.chainmaker.sdk.utils.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerUserFactory {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerUserFactory.class);

  public static User buildUserFromPrivateKeyBytes(byte[] privateKeyBytes) throws Exception {
    PrivateKey privateKey = CryptoUtils.getPrivateKeyFromBytes(privateKeyBytes);
    PublicKey publicKey = CryptoUtils.getPublicKeyFromPrivateKey(privateKey);
    byte[] publicKeyBytes =
        CryptoUtils.getPemStrFromPublicKey(publicKey).getBytes(StandardCharsets.UTF_8);

    // TODO no orgId ?
    User user = new User("");
    user.setAuthType(AuthType.Public.getMsg());
    user.setPrivateKey(privateKey);
    user.setPriBytes(privateKeyBytes);
    user.setPublicKey(publicKey);
    user.setPukBytes(publicKeyBytes);
    return user;
  }

  public static User buildUserFromPrivateKeyBytes(
      byte[] userSignKeyBytes,
      byte[] userSignCertBytes,
      byte[] tlsUserKeyBytes,
      byte[] tlsUserCertBytes,
      boolean pkcs11Enable)
      throws Exception {
    User user =
        new User(
            "",
            userSignKeyBytes,
            userSignCertBytes,
            tlsUserKeyBytes,
            tlsUserCertBytes,
            pkcs11Enable);
    user.setPriBytes(userSignKeyBytes);
    return user;
  }
}
