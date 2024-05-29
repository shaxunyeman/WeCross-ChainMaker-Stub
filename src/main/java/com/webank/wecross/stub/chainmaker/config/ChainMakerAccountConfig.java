package com.webank.wecross.stub.chainmaker.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerAccountConfig {
  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountConfig.class);

  private List<Account> account;

  public static class Account {
    private String type;
    private String orgId;
    private String name;
    private String signKeyPath;
    private String signCertPath;
    private String tlsKeyPath;
    private String tlsCertPath;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getOrgId() {
      return orgId;
    }

    public void setOrgId(String orgId) {
      this.orgId = orgId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getSignKeyPath() {
      return signKeyPath;
    }

    public void setSignKeyPath(String signKeyPath) {
      this.signKeyPath = signKeyPath;
    }

    public String getSignCertPath() {
      return signCertPath;
    }

    public void setSignCertPath(String signCertPath) {
      this.signCertPath = signCertPath;
    }

    public String getTlsKeyPath() {
      return tlsKeyPath;
    }

    public void setTlsKeyPath(String tlsKeyPath) {
      this.tlsKeyPath = tlsKeyPath;
    }

    public String getTlsCertPath() {
      return tlsCertPath;
    }

    public void setTlsCertPath(String tlsCertPath) {
      this.tlsCertPath = tlsCertPath;
    }

    @Override
    public String toString() {
      return "Account{" + "orgId='" + orgId + '\'' + ", signKeyPath='" + signKeyPath + '\'' + '}';
    }
  }

  public List<Account> getAccounts() {
    return account;
  }

  public void setAccount(List<Account> accounts) {
    this.account = accounts;
  }
}
