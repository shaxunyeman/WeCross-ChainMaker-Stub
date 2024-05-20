package com.webank.wecross.stub.chainmaker.config;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

public class ChainMakerStubConfigParserTest {
  @Test
  public void stubConfigParserTest() throws IOException {
    ChainMakerStubConfigParser chainMakerStubConfigParser =
        new ChainMakerStubConfigParser("./", "stub.toml");
    ChainMakerStubConfig chainMakerStubConfig = chainMakerStubConfigParser.loadConfig();

    ChainMakerStubConfig.Common common = chainMakerStubConfig.getCommon();
    assertTrue(Objects.nonNull(common));
    assertEquals(common.getName(), "chainmaker");
    assertEquals(common.getType(), "CHAIN_MAKER");

    ChainMakerStubConfig.Chain chain = chainMakerStubConfig.getChain();
    assertTrue(Objects.nonNull(chain));
    assertEquals(chain.getChainId(), "pkchain01");
    assertEquals(chain.getSignKeyPath(), "sign.key");
    assertEquals(chain.getAuthType(), "public");
    ChainMakerStubConfig.Chain.Crypto crypto = chain.getCrypto();
    assertTrue(Objects.nonNull(crypto));
    assertEquals(crypto.getHash(), "SHA256");
    List<ChainMakerStubConfig.Chain.Node> nodes = chain.getNodes();
    assertFalse(nodes.isEmpty());
    ChainMakerStubConfig.Chain.RpcClient rpcClient = chain.getRpcClient();
    assertTrue(Objects.nonNull(rpcClient));
    assertEquals(rpcClient.getMaxReceiveMessageSize(), 100);

    List<ChainMakerStubConfig.Resource> resources = chainMakerStubConfig.getResources();
    assertTrue(Objects.nonNull(resources) && resources.size() == 2);
    assertEquals(resources.get(0).getType(), "CM_CONTRACT");
    assertEquals(resources.get(0).getName(), "WeCrossProxy");
    assertEquals(resources.get(0).getAddress(), "16d09a2580c2ac8ed649c1df35b93061de1fa130");

    assertEquals(resources.get(1).getType(), "CM_CONTRACT");
    assertEquals(resources.get(1).getName(), "WeCrossHub");
    assertEquals(resources.get(1).getAddress(), "3928313d50222ae2ec908a99a83d5b6a56e56a7c");
  }
}
