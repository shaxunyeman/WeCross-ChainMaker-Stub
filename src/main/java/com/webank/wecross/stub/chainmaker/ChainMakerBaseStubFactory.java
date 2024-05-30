package com.webank.wecross.stub.chainmaker;

import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Driver;
import com.webank.wecross.stub.StubFactory;
import com.webank.wecross.stub.WeCrossContext;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccountFactory;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.model.CryptoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerBaseStubFactory implements StubFactory {
  private final Logger logger = LoggerFactory.getLogger(ChainMakerBaseStubFactory.class);

  private final String stubType;
  private String authType;
  private final ChainMakerAccountFactory chainMakerAccountFactory;

  public ChainMakerBaseStubFactory(String stubType) {
    this.stubType = stubType;
    this.authType = "";
    this.chainMakerAccountFactory = ChainMakerAccountFactory.getInstance(stubType);
  }

  public boolean isGMStub() {
    return StringUtils.endsWith(stubType, ChainMakerConstant.GM_STUB_SUFFIX);
  }

  @Override
  public void init(WeCrossContext weCrossContext) {}

  @Override
  public Driver newDriver() {
    CryptoSuite cryptoSuite =
        isGMStub() ? new CryptoSuite(CryptoType.SM_TYPE) : new CryptoSuite(CryptoType.ECDSA_TYPE);
    return new ChainMakerDriver(cryptoSuite);
  }

  @Override
  public Connection newConnection(String path) {
    try {
      ChainMakerConnection connection =
          ChainMakerConnectionFactory.build(path, ChainMakerConstant.STUB_TOML_NAME);
      connection.getProperties().put(ChainMakerConstant.CHAIN_MAKER_ROOT_PATH, path);

      // check proxy contract
      if (!connection.hasProxyDeployed()) {
        String errorMsg = "WeCrossProxy error: WeCrossProxy contract has not been deployed!";
        throw new Exception(errorMsg);
      }

      // check hub contract
      if (!connection.hasHubDeployed()) {
        String errorMsg = "WeCrossHub error: WeCrossHub contract has not been deployed!";
        throw new Exception(errorMsg);
      }

      this.authType =
          connection.getProperties().get(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE);

      return connection;
    } catch (Exception e) {
      logger.error(" newConnection, e: ", e);
      return null;
    }
  }

  @Override
  public Account newAccount(Map<String, Object> properties) {
    properties.put(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE, this.authType);
    return chainMakerAccountFactory.build(properties);
  }

  @Override
  public void generateAccount(String s, String[] strings) {}

  @Override
  public void generateConnection(String s, String[] strings) {}
}
