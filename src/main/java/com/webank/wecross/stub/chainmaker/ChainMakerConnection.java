package com.webank.wecross.stub.chainmaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.util.JsonFormat;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Request;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.Response;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccount;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccountFactory;
import com.webank.wecross.stub.chainmaker.client.AbstractClientWrapper;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import com.webank.wecross.stub.chainmaker.common.ChainMakerRequestType;
import com.webank.wecross.stub.chainmaker.common.ChainMakerStatusCode;
import com.webank.wecross.stub.chainmaker.common.ChainMakerStubException;
import com.webank.wecross.stub.chainmaker.common.ObjectMapperFactory;
import com.webank.wecross.stub.chainmaker.protocal.TransactionParams;
import com.webank.wecross.stub.chainmaker.utils.FunctionUtility;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.chainmaker.pb.common.ChainmakerBlock;
import org.chainmaker.pb.common.ChainmakerTransaction;
import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.utils.SdkUtils;
import org.fisco.bcos.sdk.abi.FunctionEncoder;
import org.fisco.bcos.sdk.abi.datatypes.Function;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerConnection implements Connection {
  private static final Logger logger = LoggerFactory.getLogger(ChainMakerConnection.class);
  private final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

  private List<ResourceInfo> resourceInfoList = new ArrayList<>();
  private List<ResourceInfo> resourcesCache = new ArrayList<>();
  private ConnectionEventHandler eventHandler;
  private final Map<String, String> properties = new HashMap<>();
  private final AbstractClientWrapper clientWrapper;
  private final FunctionEncoder functionEncoder;

  public ChainMakerConnection(
      CryptoSuite cryptoSuite,
      AbstractClientWrapper clientWrapper,
      ScheduledExecutorService scheduledExecutorService) {
    this.functionEncoder = new FunctionEncoder(cryptoSuite);
    this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    this.clientWrapper = clientWrapper;

    scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          if (Objects.nonNull(eventHandler)) {
            noteOnResourcesChange();
          }
        },
        10000,
        30000,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void asyncSend(Request request, Callback callback) {
    // request type
    int type = request.getType();
    if (type == ChainMakerRequestType.CALL) {
      // constantCall constantCallWithXa
      handleAsyncCallRequest(request, callback);
    } else if (type == ChainMakerRequestType.SEND_TRANSACTION) {
      // sendTransaction sendTransactionWithXa
      handleAsyncTransactionRequest(request, callback);
    } else if (type == ChainMakerRequestType.GET_BLOCK_NUMBER) {
      handleAsyncGetBlockNumberRequest(callback);
    } else if (type == ChainMakerRequestType.GET_BLOCK_BY_NUMBER) {
      handleAsyncGetBlockRequest(request, callback);
    } else if (type == ChainMakerRequestType.GET_TRANSACTION_PROOF) {
      handleAsyncGetTransactionProof(request, callback);
    } else if (type == ChainMakerRequestType.GET_TRANSACTION) {
      handleAsyncGetTransaction(request, callback);
    } else if (type == ChainMakerRequestType.CREATE_CUSTOMER_CONTRACT
        || type == ChainMakerRequestType.UPGRADE_CUSTOMER_CONTRACT) {
      handleAsyncDeployContract(request, callback);
    } else {
      logger.warn(" unrecognized request type, type: {}", request.getType());
      Response response = new Response();
      response.setErrorCode(ChainMakerStatusCode.UnrecognizedRequestType);
      response.setErrorMessage(
          ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.UnrecognizedRequestType)
              + " ,type: "
              + request.getType());
      callback.onResponse(response);
    }
  }

  private void handleAsyncGetTransaction(Request request, Callback callback) {
    Response response = new Response();
    try {
      String txId = new String(request.getData(), StandardCharsets.UTF_8);
      ChainmakerTransaction.TransactionInfo transactionInfo = clientWrapper.getTxByTxId(txId);
      if (logger.isDebugEnabled()) {
        logger.debug("handleAsyncGetTransaction: {}", JsonFormat.printer().print(transactionInfo));
      }
      response.setErrorCode(ChainMakerStatusCode.Success);
      response.setErrorMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
      response.setData(transactionInfo.toByteArray());
    } catch (Exception e) {
      logger.warn("handleAsyncGetTransaction Exception, e: ", e);
      response.setErrorCode(ChainMakerStatusCode.UnclassifiedError);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void handleAsyncDeployContract(Request request, Callback callback) {
    Response response = new Response();
    try {
      org.chainmaker.pb.common.Request.Payload payload =
          org.chainmaker.pb.common.Request.Payload.parseFrom(request.getData());

      ChainMakerAccountFactory chainMakerAccountFactory =
          ChainMakerAccountFactory.getInstance(
              getProperty(ChainMakerConstant.CHAIN_MAKER_PROPERTY_STUB_TYPE));
      List<ChainMakerAccount> accounts =
          chainMakerAccountFactory.build(
              getProperty(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE),
              getProperty(ChainMakerConstant.CHAIN_MAKER_ROOT_PATH) + File.separator + "accounts");

      org.chainmaker.pb.common.Request.EndorsementEntry[] entries =
          SdkUtils.getEndorsers(
              payload,
              accounts.stream()
                  .map(ChainMakerAccount::getUser)
                  .collect(Collectors.toList())
                  .stream()
                  .toArray(User[]::new),
              getClientWrapper().getNativeClient().getHash());

      ResultOuterClass.TxResponse chainMakerResponse =
          getClientWrapper().sendContractManageRequest(payload, entries);
      if (chainMakerResponse.getCode() == ResultOuterClass.TxStatusCode.SUCCESS) {
        response.setErrorCode(ChainMakerStatusCode.Success);
        response.setData(chainMakerResponse.toByteArray());
      } else {
        response.setErrorCode(ChainMakerStatusCode.HandleDeployContractFailed);
        response.setErrorMessage(chainMakerResponse.getContractResult().getMessage());
      }
    } catch (Exception e) {
      logger.warn("handleAsyncGetTransaction Exception, e: ", e);
      response.setErrorCode(ChainMakerStatusCode.HandleDeployContractFailed);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void handleAsyncGetTransactionProof(Request request, Callback callback) {
    // TODO no proof ？

  }

  private void handleAsyncGetBlockRequest(Request request, Callback callback) {
    Response response = new Response();
    try {
      BigInteger blockHeight = new BigInteger(request.getData());
      ChainmakerBlock.BlockInfo blockInfo = clientWrapper.getBlockByHeight(blockHeight.longValue());
      if (logger.isDebugEnabled()) {
        logger.debug("handleAsyncGetBlockRequest: {}", JsonFormat.printer().print(blockInfo));
      }
      response.setErrorCode(ChainMakerStatusCode.Success);
      response.setErrorMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
      response.setData(blockInfo.toByteArray());
    } catch (Exception e) {
      logger.warn("handleAsyncGetBlockRequest Exception, e: ", e);
      response.setErrorCode(ChainMakerStatusCode.UnclassifiedError);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void handleAsyncGetBlockNumberRequest(Callback callback) {
    Response response = new Response();
    try {
      BigInteger currentBlockHeight = BigInteger.valueOf(clientWrapper.getCurrentBlockHeight());
      if (logger.isDebugEnabled()) {
        logger.debug("currentBlockHeight: {}", currentBlockHeight);
      }
      response.setErrorCode(ChainMakerStatusCode.Success);
      response.setErrorMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
      response.setData(currentBlockHeight.toByteArray());
    } catch (Exception e) {
      logger.warn("handleGetBlockNumberRequest Exception, e: ", e);
      response.setErrorCode(ChainMakerStatusCode.HandleGetBlockNumberFailed);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void handleAsyncCallRequest(Request request, Callback callback) {
    Response response = new Response();
    try {
      TransactionParams cmRequest =
          objectMapper.readValue(request.getData(), TransactionParams.class);
      String contractAddress = cmRequest.getContractAddress();
      String contractMethodId = cmRequest.getContractMethodId();
      Map<String, byte[]> contractMethodParams = cmRequest.getContractMethodParams();
      ResultOuterClass.TxResponse txResponse =
          clientWrapper.invokeContract(contractAddress, contractMethodId, contractMethodParams);
      if (logger.isDebugEnabled()) {
        logger.debug("handleAsyncCallRequest: {}", JsonFormat.printer().print(txResponse));
      }
      response.setErrorCode(ChainMakerStatusCode.Success);
      response.setErrorMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
      response.setData(txResponse.toByteArray());
    } catch (Exception e) {
      logger.warn("handleAsyncCallRequest Exception:", e);
      response.setErrorCode(ChainMakerStatusCode.HandleCallRequestFailed);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void handleAsyncTransactionRequest(Request request, Callback callback) {
    Response response = new Response();
    try {
      TransactionParams cmRequest =
          objectMapper.readValue(request.getData(), TransactionParams.class);
      String contractAddress = cmRequest.getContractAddress();
      String contractMethodId = cmRequest.getContractMethodId();
      Map<String, byte[]> contractMethodParams = cmRequest.getContractMethodParams();
      // User user = ChainMakerUserFactory.buildUserFromPrivateKeyBytes(cmRequest.getSignKey());
      ResultOuterClass.TxResponse txResponse =
          clientWrapper.invokeContract(contractAddress, contractMethodId, contractMethodParams);
      if (logger.isDebugEnabled()) {
        logger.debug("handleAsyncTransactionRequest: {}", JsonFormat.printer().print(txResponse));
      }
      response.setErrorCode(ChainMakerStatusCode.Success);
      response.setErrorMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
      response.setData(txResponse.toByteArray());
    } catch (Exception e) {
      logger.warn("handleAsyncTransactionRequest Exception:", e);
      response.setErrorCode(ChainMakerStatusCode.HandleCallRequestFailed);
      response.setErrorMessage(e.getMessage());
    } finally {
      callback.onResponse(response);
    }
  }

  private void noteOnResourcesChange() {
    synchronized (this) {
      List<ResourceInfo> resources = getResources();
      if (!resources.equals(resourcesCache) && !resources.isEmpty()) {
        eventHandler.onResourcesChange(resources);
        resourcesCache = resources;
        if (logger.isDebugEnabled()) {
          logger.debug("resources notify, resources: {}", resources);
        }
      }
    }
  }

  public List<ResourceInfo> getResources() {
    List<ResourceInfo> resourceInfos =
        new ArrayList<ResourceInfo>() {
          {
            addAll(resourceInfoList);
          }
        };
    String[] resources = listResources();
    if (Objects.nonNull(resources)) {
      for (String resource : resources) {
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setStubType(getProperty(ChainMakerConstant.CHAIN_MAKER_PROPERTY_STUB_TYPE));
        resourceInfo.setName(resource);
        Map<Object, Object> resourceProperties = new HashMap<>();
        resourceProperties.put(
            ChainMakerConstant.CHAIN_MAKER_PROPERTY_CHAIN_ID,
            getProperty(ChainMakerConstant.CHAIN_MAKER_PROPERTY_CHAIN_ID));
        resourceProperties.put(
            ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE,
            getProperty(ChainMakerConstant.CHAIN_MAKER_PROPERTY_AUTH_TYPE));
        resourceInfo.setProperties(resourceProperties);
        resourceInfos.add(resourceInfo);
      }
    }
    return resourceInfos;
  }

  public String[] listResources() {
    try {
      Function function =
          FunctionUtility.newDefaultFunction(FunctionUtility.ProxyGetResourcesMethodName, null);
      String methodDataStr = functionEncoder.encode(function);
      String methodId =
          functionEncoder.buildMethodId(
              FunctionEncoder.buildMethodSignature(
                  function.getName(), function.getInputParameters()));
      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, methodDataStr.getBytes());
      ResultOuterClass.TxResponse txResponse =
          clientWrapper.queryContract(
              getProperty(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME), methodId, params);
      if (!Objects.equals(
          txResponse.getCode().getNumber(), ResultOuterClass.TxStatusCode.SUCCESS.getNumber())) {
        logger.warn("listPaths failed, status {}", txResponse.getCode().getNumber());
        return null;
      }
      String[] resources =
          FunctionUtility.decodeDefaultOutput(
              Hex.toHexString(txResponse.getContractResult().getResult().toByteArray()));
      Set<String> set = new LinkedHashSet<>();
      if (Objects.nonNull(resources) && resources.length != 0) {
        for (int i = resources.length - 1; i >= 0; i--) {
          set.add(resources[i]);
        }
      } else {
        logger.debug("No path found and add system resources");
      }
      return set.toArray(new String[0]);
    } catch (Exception e) {
      logger.warn("listPaths failed,", e);
      return null;
    }
  }

  public void registerCNS(String path, String address) {
    try {
      Function function = FunctionUtility.newRegisterCNSProxyFunction(path, address);
      String methodDataStr = functionEncoder.encode(function);
      String methodId =
          functionEncoder.buildMethodId(
              FunctionEncoder.buildMethodSignature(
                  function.getName(), function.getInputParameters()));
      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, methodDataStr.getBytes());
      ResultOuterClass.TxResponse txResponse =
          clientWrapper.invokeContract(
              getProperty(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME), methodId, params);
      if (!Objects.equals(
          txResponse.getCode().getNumber(), ResultOuterClass.TxStatusCode.SUCCESS.getNumber())) {
        throw new ChainMakerStubException(
            txResponse.getCode().getNumber(), txResponse.getMessage());
      }
    } catch (Exception e) {
      logger.warn("registerCNS fail,path:{},address:{}", path, address, e);
    }
  }

  public boolean hasProxyDeployed() {
    return getProperties().containsKey(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME);
  }

  public boolean hasHubDeployed() {
    return getProperties().containsKey(ChainMakerConstant.CHAIN_MAKER_HUB_NAME);
  }

  public List<ResourceInfo> getResourceInfoList() {
    return resourceInfoList;
  }

  public void setResourceInfoList(List<ResourceInfo> resourceInfoList) {
    this.resourceInfoList = resourceInfoList;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public void setConnectionEventHandler(ConnectionEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  public void addProperty(String key, String value) {
    this.properties.put(key, value);
  }

  public String getProperty(String key) {
    return this.properties.get(key);
  }

  public void addAbi(String key, String value) {
    addProperty(key + ChainMakerConstant.CHAIN_MAKER_PROPERTY_ABI_SUFFIX, value);
  }

  public String getAbi(String key) {
    return getProperty(key + ChainMakerConstant.CHAIN_MAKER_PROPERTY_ABI_SUFFIX);
  }

  public AbstractClientWrapper getClientWrapper() {
    return clientWrapper;
  }
}
