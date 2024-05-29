package com.webank.wecross.stub.chainmaker.client;

import com.webank.wecross.stub.chainmaker.config.ChainMakerStubConfig;
import java.util.Map;
import org.chainmaker.pb.common.*;
import org.chainmaker.sdk.ChainClient;
import org.chainmaker.sdk.ChainClientException;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClientWrapper implements ClientWrapper {

  private final ChainClient client;

  private final long rpcCallTimeout;
  private final long syncResultTimeout;
  private final Logger logger = LoggerFactory.getLogger(AbstractClientWrapper.class);

  public AbstractClientWrapper(ChainClient client, ChainMakerStubConfig.Chain.RpcClient rpcClient) {
    this.client = client;
    this.rpcCallTimeout = rpcClient.getCallTimeout();
    this.syncResultTimeout = rpcClient.getSyncResultTimeout();
  }

  @Override
  public ChainClient getNativeClient() {
    return client;
  }

  @Override
  public ResultOuterClass.TxResponse queryContract(
      String contractAddress, String methodId, Map<String, byte[]> params)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.queryContract(
        Numeric.cleanHexPrefix(contractAddress), methodId, null, params, rpcCallTimeout);
  }

  @Override
  public ResultOuterClass.TxResponse invokeContract(
      String contractAddress, String methodId, Map<String, byte[]> params)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.invokeContract(
        Numeric.cleanHexPrefix(contractAddress),
        methodId,
        null,
        params,
        rpcCallTimeout,
        syncResultTimeout);
  }

  @Override
  public ResultOuterClass.TxResponse invokeContractWithUser(
      String contractAddress, String methodId, Map<String, byte[]> params, User user)
      throws ChainMakerCryptoSuiteException, ChainClientException {
    Request.Payload payload =
        client.invokeContractPayload(Numeric.cleanHexPrefix(contractAddress), methodId, "", params);
    return client.sendContractRequest(payload, null, rpcCallTimeout, syncResultTimeout, user);
  }

  @Override
  public ChainmakerBlock.BlockInfo getBlockByHeight(long blockHeight)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.getBlockByHeight(blockHeight, true, rpcCallTimeout);
  }

  @Override
  public ChainmakerBlock.BlockHeader getBlockHeaderByHeight(long blockHeight)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.getBlockHeaderByHeight(blockHeight, rpcCallTimeout);
  }

  @Override
  public long getCurrentBlockHeight() throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.getCurrentBlockHeight(rpcCallTimeout);
  }

  @Override
  public ChainmakerTransaction.TransactionInfo getTxByTxId(String txId)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.getTxByTxId(txId, rpcCallTimeout);
  }

  @Override
  public byte[] getMerklePathByTxId(String txId)
      throws ChainClientException, ChainMakerCryptoSuiteException {
    return client.getMerklePathByTxId(txId, rpcCallTimeout);
  }

  @Override
  public Request.Payload createContractCreatePayload(
      String contractName,
      String version,
      byte[] byteCode,
      ContractOuterClass.RuntimeType runtime,
      Map<String, byte[]> params)
      throws ChainMakerCryptoSuiteException {
    return client.createContractCreatePayload(contractName, version, byteCode, runtime, params);
  }

  @Override
  public Request.Payload createContractUpgradePayload(
      String contractName,
      String version,
      byte[] byteCode,
      ContractOuterClass.RuntimeType runtime,
      Map<String, byte[]> params)
      throws ChainMakerCryptoSuiteException {
    return client.createContractUpgradePayload(contractName, version, byteCode, runtime, params);
  }

  @Override
  public ResultOuterClass.TxResponse sendContractManageRequest(
      Request.Payload payload, Request.EndorsementEntry[] endorsementEntries)
      throws ChainMakerCryptoSuiteException, ChainClientException {
    return client.sendContractManageRequest(
        payload, endorsementEntries, rpcCallTimeout, syncResultTimeout);
  }
}
