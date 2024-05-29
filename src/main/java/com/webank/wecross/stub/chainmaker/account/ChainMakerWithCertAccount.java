package com.webank.wecross.stub.chainmaker.account;

import org.chainmaker.pb.config.ChainConfigOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.utils.CryptoUtils;

public class ChainMakerWithCertAccount extends ChainMakerAccount {

  public ChainMakerWithCertAccount(String name, String type, User user) {
    super(name, type, user);
  }

  @Override
  public String getIdentity() {
    String id = null;
    try {
      id =
          CryptoUtils.certToAddrStr(
              this.getUser().getCertificate(), ChainConfigOuterClass.AddrType.ETHEREUM);
    } catch (Exception e) {
      id = "";
    }
    return id;
  }
}
