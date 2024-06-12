package com.webank.wecross.stub.chainmaker.preparation;

import com.webank.wecross.stub.chainmaker.ChainMakerConnection;
import com.webank.wecross.stub.chainmaker.ChainMakerConnectionFactory;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccount;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccountFactory;
import com.webank.wecross.stub.chainmaker.config.ChainMakerStubConfig;
import com.webank.wecross.stub.chainmaker.config.ChainMakerStubConfigParser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.chainmaker.pb.common.ContractOuterClass;
import org.chainmaker.pb.common.Request;
import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.utils.FileUtils;
import org.chainmaker.sdk.utils.SdkUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class DeployWeCrossContract {
  private final String WECROSS_PROXY = "WeCrossProxy";
  private final String WECROSS_HUB = "WeCrossHub";
  private final String EVM_CONTRACT_PATH = "contracts/evm";

  private ChainMakerConnection chainMakerConnection;
  private String chainPath;
  private List<User> endorsementUsers = new ArrayList<>();

  private DeployWeCrossContract(
      ChainMakerConnection chainMakerConnection, List<User> endorsementUsers, String chainPath) {
    this.chainMakerConnection = chainMakerConnection;
    this.endorsementUsers = endorsementUsers;
    this.chainPath = chainPath;
  }

  private void deployOrUpgradeProxyContract(boolean deploy, String contractName) throws Exception {
    String contractBinFile =
        this.chainPath
            + File.separator
            + EVM_CONTRACT_PATH
            + File.separator
            + contractName
            + File.separator
            + contractName
            + ".bin";

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource resource = resolver.getResource(contractBinFile);
    byte[] byteCodes = FileUtils.getFileBytes(resource.getFile().getAbsolutePath());
    String version = String.valueOf(System.currentTimeMillis() / 1000);

    Request.Payload payload = null;
    if (deploy) {
      payload =
          chainMakerConnection
              .getClientWrapper()
              .createContractCreatePayload(
                  contractName, version, byteCodes, ContractOuterClass.RuntimeType.EVM, null);
    } else {
      payload =
          chainMakerConnection
              .getClientWrapper()
              .createContractUpgradePayload(
                  contractName, version, byteCodes, ContractOuterClass.RuntimeType.EVM, null);
    }

    Request.EndorsementEntry[] endorsementEntries =
        SdkUtils.getEndorsers(
            payload,
            endorsementUsers.stream().toArray(User[]::new),
            chainMakerConnection.getClientWrapper().getNativeClient().getHash());

    ResultOuterClass.TxResponse response =
        chainMakerConnection
            .getClientWrapper()
            .sendContractManageRequest(payload, endorsementEntries);
    if (response.getCode() == ResultOuterClass.TxStatusCode.SUCCESS) {
      System.out.println(
          "SUCCESS: "
              + contractName
              + ": "
              + version
              + " has been "
              + (deploy ? "deployed!" : "upgraded")
              + " chain: "
              + chainPath);
    } else {
      throw new RuntimeException(
          (deploy ? " deploy" : " upgrade")
              + " contract failed, error code: "
              + response.getCode()
              + ", hash: "
              + chainMakerConnection.getClientWrapper().getNativeClient().getHash());
    }
  }

  public static DeployWeCrossContract build(String chainPath, String configName) throws Exception {
    ChainMakerStubConfigParser chainMakerStubConfigParser =
        new ChainMakerStubConfigParser(chainPath, configName);
    ChainMakerStubConfig chainMakerStubConfig = chainMakerStubConfigParser.loadConfig();
    ChainMakerConnection connection = ChainMakerConnectionFactory.build(chainPath, configName);
    ChainMakerAccountFactory chainMaker =
        ChainMakerAccountFactory.getInstance(chainMakerStubConfig.getCommon().getType());
    List<ChainMakerAccount> accounts =
        chainMaker.build(
            chainMakerStubConfig.getChain().getAuthType(), chainPath + File.separator + "accounts");
    DeployWeCrossContract deployWeCrossContract =
        new DeployWeCrossContract(
            connection,
            accounts.stream().map(ChainMakerAccount::getUser).collect(Collectors.toList()),
            chainPath);
    return deployWeCrossContract;
  }

  public void deployWeCrossProxy(boolean deploy) throws Exception {
    deployOrUpgradeProxyContract(deploy, WECROSS_PROXY);
  }

  public void deployWeCrossHub(boolean deploy) throws Exception {
    deployOrUpgradeProxyContract(deploy, WECROSS_HUB);
  }
}
