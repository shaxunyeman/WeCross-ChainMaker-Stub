package com.webank.wecross.stub.chainmaker.account;

import org.chainmaker.pb.config.ChainConfigOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.utils.CryptoUtils;

public class ChainMakerPublicAccount extends ChainMakerAccount {

  public ChainMakerPublicAccount(String name, String type, User user) {
    super(name, type, user);
  }

  @Override
  public String getIdentity() {
    return CryptoUtils.pkToAddrStr(
        this.getUser().getPublicKey(), ChainConfigOuterClass.AddrType.ETHEREUM, "");
  }
}
