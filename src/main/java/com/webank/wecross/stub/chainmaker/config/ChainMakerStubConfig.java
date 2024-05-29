package com.webank.wecross.stub.chainmaker.config;

import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.chainmaker.pb.config.ChainConfigOuterClass;
import org.chainmaker.sdk.utils.CryptoUtils;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerStubConfig {
  private static final Logger logger = LoggerFactory.getLogger(ChainMakerStubConfig.class);

  private Common common;
  private Chain chain;
  private List<Resource> resources;

  public boolean isGMStub() {
    return StringUtils.endsWith(common.getType(), ChainMakerConstant.GM_STUB_SUFFIX);
  }

  public CryptoSuite getStubCryptoSuite() {
    return isGMStub()
        ? new CryptoSuite(CryptoType.SM_TYPE)
        : new CryptoSuite(CryptoType.ECDSA_TYPE);
  }

  public static class Common {
    private String name;
    private String type;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return "Common{" + "name='" + name + '\'' + ", type='" + type + '\'' + '}';
    }
  }

  public static class Chain {
    private String chainId;
    private String orgId;
    private String signKeyPath;
    private String signCertPath;
    private String tlsKeyPath;
    private String tlsCertPath;
    private String authType;
    private Crypto crypto;
    private List<Node> nodes;
    private RpcClient rpcClient;

    public static class Crypto {
      private String hash;

      public String getHash() {
        return hash;
      }

      public void setHash(String hash) {
        this.hash = hash;
      }

      @Override
      public String toString() {
        return "Crypto{" + "hash='" + hash + '\'' + '}';
      }
    }

    public static class Node {
      private String nodeAddr;
      private Boolean enableTls = false;
      private String tlsHostName = null;
      private List<String> trustRootPaths = new ArrayList<>();

      public String getNodeAddr() {
        return nodeAddr;
      }

      public void setNodeAddr(String nodeAddr) {
        this.nodeAddr = nodeAddr;
      }

      public Boolean getEnableTls() {
        return enableTls;
      }

      public void setEnableTls(Boolean enableTls) {
        this.enableTls = enableTls;
      }

      public String getTlsHostName() {
        return tlsHostName;
      }

      public void setTlsHostName(String tlsHostName) {
        this.tlsHostName = tlsHostName;
      }

      public List<String> getTrustRootPaths() {
        return trustRootPaths;
      }

      public void setTrustRootPaths(List<String> trustRootPaths) {
        this.trustRootPaths = trustRootPaths;
      }

      @Override
      public String toString() {
        return "Node{" + "nodeAddr='" + nodeAddr + '\'' + '}';
      }
    }

    public static class RpcClient {
      private long callTimeout = 10000;
      private long syncResultTimeout = 10000;
      private int maxReceiveMessageSize = 100;

      public long getCallTimeout() {
        return callTimeout;
      }

      public void setCallTimeout(long callTimeout) {
        this.callTimeout = callTimeout;
      }

      public long getSyncResultTimeout() {
        return syncResultTimeout;
      }

      public void setSyncResultTimeout(long syncResultTimeout) {
        this.syncResultTimeout = syncResultTimeout;
      }

      public int getMaxReceiveMessageSize() {
        return maxReceiveMessageSize;
      }

      public void setMaxReceiveMessageSize(int maxReceiveMessageSize) {
        this.maxReceiveMessageSize = maxReceiveMessageSize;
      }

      @Override
      public String toString() {
        return "RpcClient{"
            + "callTimeout="
            + callTimeout
            + ", syncResultTimeout="
            + syncResultTimeout
            + ", maxReceiveMessageSize="
            + maxReceiveMessageSize
            + '}';
      }
    }

    public String getChainId() {
      return chainId;
    }

    public void setChainId(String chainId) {
      this.chainId = chainId;
    }

    public String getOrgId() {
      return orgId;
    }

    public void setOrgId(String orgId) {
      this.orgId = orgId;
    }

    public String getSignKeyPath() {
      return signKeyPath;
    }

    public void setSignKeyPath(String signKeyPath) {
      this.signKeyPath = signKeyPath;
    }

    public String getSignCertPath() {
      return signCertPath;
    }

    public void setSignCertPath(String signCertPath) {
      this.signCertPath = signCertPath;
    }

    public String getTlsKeyPath() {
      return tlsKeyPath;
    }

    public void setTlsKeyPath(String tlsKeyPath) {
      this.tlsKeyPath = tlsKeyPath;
    }

    public String getTlsCertPath() {
      return tlsCertPath;
    }

    public void setTlsCertPath(String tlsCertPath) {
      this.tlsCertPath = tlsCertPath;
    }

    public String getAuthType() {
      return authType;
    }

    public void setAuthType(String authType) {
      this.authType = authType;
    }

    public Crypto getCrypto() {
      return crypto;
    }

    public void setCrypto(Crypto crypto) {
      this.crypto = crypto;
    }

    public List<Node> getNodes() {
      return nodes;
    }

    public void setNodes(List<Node> nodes) {
      this.nodes = nodes;
    }

    public RpcClient getRpcClient() {
      return rpcClient;
    }

    public void setRpcClient(RpcClient rpcClient) {
      this.rpcClient = rpcClient;
    }
  }

  public static class Resource {
    private String type;
    private String name;
    private String address;
    private String abi;

    public String getAddress() {
      if (StringUtils.isNotBlank(address)) {
        // cleanHexPrefix 0x
        return Numeric.cleanHexPrefix(address);
      }
      // calculate address based on contract name
      return CryptoUtils.nameToAddrStr(name, ChainConfigOuterClass.AddrType.ETHEREUM);
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public String getAbi() {
      return abi;
    }

    public void setAbi(String abi) {
      this.abi = abi;
    }

    @Override
    public String toString() {
      return "Resource{"
          + "type='"
          + type
          + '\''
          + ", name='"
          + name
          + '\''
          + ", address='"
          + address
          + '\''
          + ", abi='"
          + abi
          + '\''
          + '}';
    }
  }

  public Common getCommon() {
    return common;
  }

  public void setCommon(Common common) {
    this.common = common;
  }

  public Chain getChain() {
    return chain;
  }

  public void setChain(Chain chain) {
    this.chain = chain;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  public List<ResourceInfo> convertToResourceInfos() {
    List<ResourceInfo> resourceInfos = new ArrayList<>();
    for (Resource resource : this.getResources()) {
      ResourceInfo resourceInfo = new ResourceInfo();
      resourceInfo.setName(resource.getName());
      resourceInfo.setStubType(this.getCommon().getType());
      resourceInfo.getProperties().put(resource.getName(), resource.getAddress());
      resourceInfo
          .getProperties()
          .put(
              resource.getName() + ChainMakerConstant.CHAIN_MAKER_PROPERTY_ABI_SUFFIX,
              resource.getAbi());
      resourceInfo
          .getProperties()
          .put(ChainMakerConstant.CHAIN_MAKER_PROPERTY_CHAIN_ID, this.getChain().getChainId());
      resourceInfo
          .getProperties()
          .put(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE, this.getChain().getAuthType());
      resourceInfos.add(resourceInfo);
    }
    logger.info(" resource list: {}", resourceInfos);
    return resourceInfos;
  }
}
