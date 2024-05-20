package com.webank.wecross.stub.chainmaker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerAccountConfig {
  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountConfig.class);

  private Account account;

  public static class Account {
    private String type;
    private String accountFile;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getAccountFile() {
      return accountFile;
    }

    public void setAccountFile(String accountFile) {
      this.accountFile = accountFile;
    }

    @Override
    public String toString() {
      return "Account{" + "type='" + type + '\'' + ", accountFile='" + accountFile + '\'' + '}';
    }
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }
}
