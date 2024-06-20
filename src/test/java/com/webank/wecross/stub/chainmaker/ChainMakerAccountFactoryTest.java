package com.webank.wecross.stub.chainmaker;

import static junit.framework.TestCase.assertEquals;

import com.webank.wecross.stub.chainmaker.account.ChainMakerAccount;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccountFactory;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import java.util.List;
import org.chainmaker.sdk.config.AuthType;
import org.junit.Test;

public class ChainMakerAccountFactoryTest {
  @Test
  public void buildAccountTest() throws Exception {
    ChainMakerAccountFactory chainMaker =
        ChainMakerAccountFactory.getInstance(ChainMakerConstant.CHAIN_MAKER_ECDSA_EVM_STUB_TYPE);
    List<ChainMakerAccount> accounts =
        chainMaker.build(AuthType.Public.getMsg(), "classpath:/accounts");
    assertEquals(accounts.size(), 1);
    assertEquals(accounts.get(0).getType(), ChainMakerConstant.CHAIN_MAKER_ECDSA_EVM_STUB_TYPE);
    assertEquals(accounts.get(0).getName(), "chain_maker");
  }
}
