package com.webank.wecross.stub.chainmaker;

import static com.webank.wecross.stub.chainmaker.common.ChainMakerConstant.CHAIN_MAKER_GM_EVM_STUB_TYPE;

import com.webank.wecross.stub.Stub;

@Stub(CHAIN_MAKER_GM_EVM_STUB_TYPE)
public class ChainMakerGMStubFactory extends ChainMakerBaseStubFactory {

  public ChainMakerGMStubFactory() {
    super(CHAIN_MAKER_GM_EVM_STUB_TYPE);
  }

  public static void main(String[] args) {
    System.out.println("init ChainMakerGMStubFactory");
  }
}
