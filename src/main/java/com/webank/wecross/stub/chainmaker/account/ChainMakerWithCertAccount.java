package com.webank.wecross.stub.chainmaker.account;

import org.chainmaker.pb.config.ChainConfigOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.utils.CryptoUtils;

public class ChainMakerWithCertAccount extends ChainMakerAccount {

  private byte[] tlsKey;
  private byte[] tlsCert;

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

  public byte[] getTlsKey() {
    return tlsKey;
  }

  public void setTlsKey(byte[] tlsKey) {
    this.tlsKey = tlsKey;
  }

  public byte[] getTlsCert() {
    return tlsCert;
  }

  public void setTlsCert(byte[] tlsCert) {
    this.tlsCert = tlsCert;
  }
}
