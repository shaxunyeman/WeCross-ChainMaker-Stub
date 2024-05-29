package com.webank.wecross.stub.chainmaker.integration.preparation;

import com.webank.wecross.stub.chainmaker.preparation.DeployWeCrossContract;
import org.junit.Test;

public class DeployWeCrossContractTest {
  @Test
  public void deployWeCrossProxyTest() throws Exception {
    DeployWeCrossContract weCross = DeployWeCrossContract.build("./", "stub.toml");
    weCross.deployWeCrossProxy(true);
  }
}
