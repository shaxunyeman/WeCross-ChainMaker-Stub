package com.webank.wecross.stub.chainmaker.account;

import com.webank.wecross.stub.Account;
import org.chainmaker.sdk.User;

public class ChainMakerAccount implements Account {
  private final String name;
  private final String type;
  private final User user;
  private int keyID;
  private boolean isDefault;

  public ChainMakerAccount(String name, String type, User user) {
    this.name = name;
    this.type = type;
    this.user = user;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getIdentity() {
    return "";
  }

  @Override
  public int getKeyID() {
    return keyID;
  }

  @Override
  public boolean isDefault() {
    return isDefault;
  }

  public User getUser() {
    return user;
  }
}
