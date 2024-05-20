package com.webank.wecross.stub.chainmaker;

import static junit.framework.TestCase.assertEquals;

import com.webank.wecross.stub.chainmaker.account.ChainMakerAccountFactory;
import com.webank.wecross.stub.chainmaker.account.ChainMakerPublicAccount;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.junit.Test;

public class ChainMakerAccountFactoryTest {
  @Test
  public void buildAccountTest()
      throws IOException, ChainMakerCryptoSuiteException, NoSuchAlgorithmException,
          InvalidKeySpecException, NoSuchProviderException {
    ChainMakerAccountFactory chainMaker =
        ChainMakerAccountFactory.getInstance(ChainMakerConstant.CHAIN_MAKER_ECDSA_EVM_STUB_TYPE);
    ChainMakerPublicAccount account = chainMaker.build("chain_maker", "classpath:/accounts");

    assertEquals(account.getIdentity(), "439dea8f34bc33f61ef56151319769e209f83b66");
    assertEquals(account.getType(), ChainMakerConstant.CHAIN_MAKER_ECDSA_EVM_STUB_TYPE);
    assertEquals(account.getName(), "chain_maker");
  }
}
