package com.webank.wecross.stub.chainmaker.client;

import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.stub.chainmaker.config.ChainMakerStubConfig;
import java.io.IOException;
import java.util.List;
import org.chainmaker.sdk.ChainClient;
import org.chainmaker.sdk.ChainManager;
import org.chainmaker.sdk.config.AuthType;
import org.chainmaker.sdk.config.ChainClientConfig;
import org.chainmaker.sdk.config.CryptoConfig;
import org.chainmaker.sdk.config.NodeConfig;
import org.chainmaker.sdk.config.RpcClientConfig;
import org.chainmaker.sdk.config.SdkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ClientUtility {

  private static final Logger logger = LoggerFactory.getLogger(ClientUtility.class);

  private static final ChainManager chainManager;

  static {
    chainManager = ChainManager.getInstance();
  }

  public static ChainClient initClient(ChainMakerStubConfig stubConfig) throws Exception {
    SdkConfig sdkConfig = new SdkConfig();
    ChainClientConfig chainClientConfig = buildChainClientConfig(stubConfig.getChain());
    sdkConfig.setChainClient(chainClientConfig);
    return chainManager.createChainClient(sdkConfig);
  }

  private static ChainClientConfig buildChainClientConfig(ChainMakerStubConfig.Chain chain)
      throws WeCrossException, IOException {
    String chainId = chain.getChainId();
    String orgId = chain.getOrgId();
    String signKeyPath = chain.getSignKeyPath();
    String signCertPath = chain.getSignCertPath();
    String tlsKeyPath = chain.getTlsKeyPath();
    String tlsCertPath = chain.getTlsCertPath();
    String authType = chain.getAuthType();
    String hash = chain.getCrypto().getHash();
    List<ChainMakerStubConfig.Chain.Node> nodes = chain.getNodes();
    int maxReceiveMessageSize = chain.getRpcClient().getMaxReceiveMessageSize();

    CryptoConfig cryptoConfig = new CryptoConfig();
    cryptoConfig.setHash(hash);

    NodeConfig[] nodeConfigs =
        nodes.stream()
            .map(
                s -> {
                  NodeConfig nodeConfig = new NodeConfig();
                  nodeConfig.setNodeAddr(s.getNodeAddr());
                  nodeConfig.setEnable_tls(s.getEnableTls());
                  nodeConfig.setTls_host_name(s.getTlsHostName());
                  nodeConfig.setTrust_root_paths(s.getTrustRootPaths().toArray(new String[0]));
                  return nodeConfig;
                })
            .toArray(NodeConfig[]::new);

    RpcClientConfig rpcClientConfig = new RpcClientConfig();
    rpcClientConfig.setMaxReceiveMessageSize(maxReceiveMessageSize);

    ChainClientConfig chainClientConfig = new ChainClientConfig();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    if (authType.equals(AuthType.PermissionedWithCert.getMsg())) {
      Resource signKeyResource = resolver.getResource(signKeyPath);
      if (!signKeyResource.exists() || !signKeyResource.isFile()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.DIR_NOT_EXISTS,
            signKeyPath + " does not exist, please check.");
      }

      Resource signCertResource = resolver.getResource(signCertPath);
      if (!signCertResource.exists() || !signCertResource.isFile()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.DIR_NOT_EXISTS,
            signCertPath + " does not exist, please check.");
      }

      Resource tlsKeyResource = resolver.getResource(tlsKeyPath);
      if (!tlsKeyResource.exists() || !tlsKeyResource.isFile()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.DIR_NOT_EXISTS,
            tlsKeyPath + " does not exist, please check.");
      }

      Resource tlsCertResource = resolver.getResource(tlsCertPath);
      if (!tlsCertResource.exists() || !tlsCertResource.isFile()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.DIR_NOT_EXISTS,
            tlsCertPath + " does not exist, please check.");
      }

      if (orgId == null || orgId.isEmpty()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.FIELD_MISSING, "orgId does not exist, please check.");
      }

      chainClientConfig.setUserSignKeyFilePath(signKeyResource.getFile().getPath());
      chainClientConfig.setUserSignCrtFilePath(signCertResource.getFile().getPath());
      chainClientConfig.setUserKeyFilePath(tlsKeyResource.getFile().getPath());
      chainClientConfig.setUserCrtFilePath(tlsCertResource.getFile().getPath());
      chainClientConfig.setOrgId(orgId);

    } else {
      Resource signKeyResource = resolver.getResource(signKeyPath);
      if (!signKeyResource.exists() || !signKeyResource.isFile()) {
        throw new WeCrossException(
            WeCrossException.ErrorCode.DIR_NOT_EXISTS,
            signKeyPath + " does not exist, please check.");
      }
      chainClientConfig.setUserSignKeyFilePath(signKeyResource.getFile().getPath());
    }

    chainClientConfig.setChainId(chainId);
    chainClientConfig.setAuthType(authType);
    chainClientConfig.setCrypto(cryptoConfig);
    chainClientConfig.setNodes(nodeConfigs);
    chainClientConfig.setRpcClient(rpcClientConfig);

    return chainClientConfig;
  }
}
