package com.webank.wecross.stub.chainmaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.Block;
import com.webank.wecross.stub.BlockManager;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Driver;
import com.webank.wecross.stub.Path;
import com.webank.wecross.stub.Request;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.StubConstant;
import com.webank.wecross.stub.Transaction;
import com.webank.wecross.stub.TransactionContext;
import com.webank.wecross.stub.TransactionException;
import com.webank.wecross.stub.TransactionRequest;
import com.webank.wecross.stub.TransactionResponse;
import com.webank.wecross.stub.chainmaker.account.ChainMakerAccount;
import com.webank.wecross.stub.chainmaker.account.ChainMakerPublicAccount;
import com.webank.wecross.stub.chainmaker.common.ChainMakerConstant;
import com.webank.wecross.stub.chainmaker.common.ChainMakerRequestType;
import com.webank.wecross.stub.chainmaker.common.ChainMakerStatusCode;
import com.webank.wecross.stub.chainmaker.common.ChainMakerStubException;
import com.webank.wecross.stub.chainmaker.common.ObjectMapperFactory;
import com.webank.wecross.stub.chainmaker.protocal.TransactionParams;
import com.webank.wecross.stub.chainmaker.protocal.TransactionProof;
import com.webank.wecross.stub.chainmaker.utils.BlockUtility;
import com.webank.wecross.stub.chainmaker.utils.FunctionUtility;
import com.webank.wecross.stub.chainmaker.utils.RevertMessage;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.chainmaker.pb.common.ChainmakerBlock;
import org.chainmaker.pb.common.ChainmakerTransaction;
import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.User;
import org.chainmaker.sdk.config.AuthType;
import org.chainmaker.sdk.utils.CryptoUtils;
import org.fisco.bcos.sdk.abi.ABICodec;
import org.fisco.bcos.sdk.abi.FunctionEncoder;
import org.fisco.bcos.sdk.abi.datatypes.Function;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple4;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple6;
import org.fisco.bcos.sdk.abi.wrapper.ABICodecJsonWrapper;
import org.fisco.bcos.sdk.abi.wrapper.ABIDefinition;
import org.fisco.bcos.sdk.abi.wrapper.ABIDefinitionFactory;
import org.fisco.bcos.sdk.abi.wrapper.ABIObject;
import org.fisco.bcos.sdk.abi.wrapper.ABIObjectFactory;
import org.fisco.bcos.sdk.abi.wrapper.ContractABIDefinition;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.utils.Hex;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerDriver implements Driver {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerDriver.class);

  private final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

  private final ABICodecJsonWrapper codecJsonWrapper;

  private final ABICodec abiCodec;

  private final FunctionEncoder functionEncoder;

  private final ABIDefinitionFactory abiDefinitionFactory;

  public ChainMakerDriver(CryptoSuite cryptoSuite) {
    this.codecJsonWrapper = new ABICodecJsonWrapper(true);
    this.abiCodec = new ABICodec(cryptoSuite, true);
    this.functionEncoder = new FunctionEncoder(cryptoSuite);
    this.abiDefinitionFactory = new ABIDefinitionFactory(cryptoSuite);
  }

  @Override
  public ImmutablePair<Boolean, TransactionRequest> decodeTransactionRequest(Request request) {
    int requestType = request.getType();
    /** check if transaction request */
    if ((requestType != ChainMakerRequestType.CALL)
        && (requestType != ChainMakerRequestType.SEND_TRANSACTION)) {
      return new ImmutablePair<>(false, null);
    }
    try {
      byte[] data = request.getData();
      TransactionParams transactionParams = objectMapper.readValue(data, TransactionParams.class);
      TransactionRequest transactionRequest = transactionParams.getTransactionRequest();
      String[] args = transactionRequest.getArgs();
      String method = transactionRequest.getMethod();
      TransactionParams.SUB_TYPE subType = transactionParams.getSubType();
      String contractAbi = transactionParams.getAbi();
      String encodedFromInput = "";
      String encodedFromNow = "";
      switch (subType) {
        case SEND_TX_BY_PROXY:
        case CALL_BY_PROXY:
          {
            byte[] contractMethodParamsBytes =
                transactionParams
                    .getContractMethodParams()
                    .get(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM);
            String input =
                Numeric.cleanHexPrefix(
                    new String(contractMethodParamsBytes, StandardCharsets.UTF_8));
            if (subType == TransactionParams.SUB_TYPE.SEND_TX_BY_PROXY) {
              if (input.startsWith(
                  Numeric.cleanHexPrefix(
                      functionEncoder.buildMethodId(
                          FunctionUtility.ProxySendTransactionTXMethod)))) {
                // sendTransactionWithXa
                Tuple6<String, String, BigInteger, String, String, byte[]>
                    sendTransactionProxyFunctionInput =
                        FunctionUtility.getSendTransactionProxyFunctionInput(input);
                encodedFromInput = Hex.toHexString(sendTransactionProxyFunctionInput.getValue6());
              } else {
                // sendTransaction
                Tuple3<String, String, byte[]> sendTransactionProxyFunctionInput =
                    FunctionUtility.getSendTransactionProxyWithoutTxIdFunctionInput(input);
                String encodedMethodWithArgs =
                    Hex.toHexString(sendTransactionProxyFunctionInput.getValue3());
                encodedFromInput = encodedMethodWithArgs.substring(FunctionUtility.MethodIDLength);
              }
            } else {
              if (input.startsWith(
                  Numeric.cleanHexPrefix(
                      functionEncoder.buildMethodId(
                          FunctionUtility.ProxyCallWithTransactionIdMethod)))) {
                // constantCallWithXa
                Tuple4<String, String, String, byte[]> constantCallProxyFunctionInput =
                    FunctionUtility.getConstantCallProxyFunctionInput(input);
                encodedFromInput = Hex.toHexString(constantCallProxyFunctionInput.getValue4());
              } else {
                // constantCall
                Tuple2<String, byte[]> sendTransactionProxyFunctionInput =
                    FunctionUtility.getConstantCallFunctionInput(input);
                String encodedMethodWithArgs =
                    Hex.toHexString(sendTransactionProxyFunctionInput.getValue2());
                encodedFromInput = encodedMethodWithArgs.substring(FunctionUtility.MethodIDLength);
              }
            }
            // encoded now
            List<ABIDefinition> abiDefinitions =
                abiDefinitionFactory
                    .loadABI(transactionParams.getAbi())
                    .getFunctions()
                    .get(transactionRequest.getMethod());
            if (Objects.isNull(abiDefinitions) || abiDefinitions.isEmpty()) {
              throw new InvalidParameterException(
                  " found no method in encodedFromInput, method: "
                      + transactionRequest.getMethod());
            }
            encodedFromNow = encodeFunctionArgs(abiDefinitions.get(0), args);
            break;
          }
        case SEND_TX:
        case CALL:
          {
            byte[] contractMethodParamsBytes =
                transactionParams
                    .getContractMethodParams()
                    .get(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM);
            encodedFromInput = new String(contractMethodParamsBytes, StandardCharsets.UTF_8);
            encodedFromNow =
                abiCodec.encodeMethodFromString(
                    contractAbi, method, args != null ? Arrays.asList(args) : new ArrayList<>());
            break;
          }
        default:
          {
            // not call/sendTransaction
            return new ImmutablePair<>(true, null);
          }
      }
      // compare
      if (Numeric.cleanHexPrefix(encodedFromNow).equals(Numeric.cleanHexPrefix(encodedFromInput))) {
        return new ImmutablePair<>(true, transactionRequest);
      }
      logger.warn(
          " encodedFromInput not meet expectations, encodedFromInput:{}, encodedFromNow:{}",
          encodedFromInput,
          encodedFromNow);
      return new ImmutablePair<>(true, null);
    } catch (Exception e) {
      logger.error("decodeTransactionRequest error: ", e);
      return new ImmutablePair<>(true, null);
    }
  }

  @Override
  public List<ResourceInfo> getResources(Connection connection) {
    if (connection instanceof ChainMakerConnection) {
      return ((ChainMakerConnection) connection).getResources();
    }
    logger.error("Not ChainMaker connection, connection name: {}", connection.getClass().getName());
    return new ArrayList<>();
  }

  @Override
  public void asyncCall(
      TransactionContext context,
      TransactionRequest request,
      boolean byProxy,
      Connection connection,
      Callback callback) {
    if (byProxy) {
      asyncCallByProxy(context, request, connection, callback);
    } else {
      asyncCallNative(context, request, connection, callback);
    }
  }

  @Override
  public void asyncSendTransaction(
      TransactionContext context,
      TransactionRequest request,
      boolean byProxy,
      Connection connection,
      Callback callback) {
    if (byProxy) {
      asyncSendTransactionByProxy(context, request, connection, callback);
    } else {
      asyncSendTransactionNative(context, request, connection, callback);
    }
  }

  @Override
  public void asyncGetBlockNumber(Connection connection, GetBlockNumberCallback callback) {
    Request request = Request.newRequest(ChainMakerRequestType.GET_BLOCK_NUMBER, "");
    connection.asyncSend(
        request,
        response -> {
          if (response.getErrorCode() != ChainMakerStatusCode.Success) {
            logger.warn(
                " errorCode: {},  errorMessage: {}",
                response.getErrorCode(),
                response.getErrorMessage());
            callback.onResponse(new Exception(response.getErrorMessage()), -1);
          } else {
            BigInteger blockNumber = new BigInteger(response.getData());
            logger.debug(" blockNumber: {}", blockNumber);
            callback.onResponse(null, blockNumber.longValue());
          }
        });
  }

  @Override
  public void asyncGetBlock(
      long blockNumber, boolean onlyHeader, Connection connection, GetBlockCallback callback) {
    byte[] blockNumberBytes = BigInteger.valueOf(blockNumber).toByteArray();
    Request request =
        Request.newRequest(ChainMakerRequestType.GET_BLOCK_BY_NUMBER, blockNumberBytes);
    connection.asyncSend(
        request,
        response -> {
          if (response.getErrorCode() != ChainMakerStatusCode.Success) {
            logger.warn(
                " asyncGetBlock, errorCode: {},  errorMessage: {}",
                response.getErrorCode(),
                response.getErrorMessage());

            callback.onResponse(new Exception(response.getErrorMessage()), null);
          } else {
            try {
              ChainmakerBlock.BlockInfo blockInfo =
                  ChainmakerBlock.BlockInfo.parseFrom(response.getData());
              Block block = BlockUtility.convertToBlock(blockInfo, onlyHeader);
              block.setRawBytes(response.getData());
              if (!block.getTransactionsHashes().isEmpty()) {
                List<ChainmakerTransaction.Transaction> txsList = blockInfo.getBlock().getTxsList();
                for (ChainmakerTransaction.Transaction chainMakerTx : txsList) {
                  parseChainMakerTransaction(chainMakerTx, block, connection);
                }
              }
              // TODO: block header validation

              callback.onResponse(null, block);
            } catch (Exception e) {
              logger.warn(" blockNumber: {}, e: ", blockNumber, e);
              callback.onResponse(e, null);
            }
          }
        });
  }

  private void parseChainMakerTransaction(
      ChainmakerTransaction.Transaction chainmakerTransaction, Block block, Connection connection)
      throws Exception {
    org.chainmaker.pb.common.Request.Payload payload = chainmakerTransaction.getPayload();
    String methodId;
    String input;
    String xaTransactionID = "0";
    String path;
    String resource;
    Transaction transaction = new Transaction();
    String txId = payload.getTxId();
    transaction.setTxBytes(chainmakerTransaction.toByteArray());
    String memberInfo =
        chainmakerTransaction.getSender().getSigner().getMemberInfo().toStringUtf8();
    String hashType =
        connection.getProperties().get(ChainMakerConstant.CHAIN_MAKER_PROPERTY_CRYPTO_HASH);
    String algorithm =
        ((ChainMakerConnection) connection)
            .getClientWrapper()
            .getNativeClient()
            .getClientUser()
            .getPrivateKey()
            .getAlgorithm();
    String userAddr = CryptoUtils.getEVMAddressFromPKPEM(memberInfo, hashType, algorithm);
    transaction.setAccountIdentity(userAddr);
    transaction.setTransactionByProxy(true);
    transaction.getTransactionResponse().setHash(txId);
    transaction.getTransactionResponse().setBlockNumber(block.getBlockHeader().getNumber());
    if (block.getBlockHeader() != null) {
      transaction.getTransactionResponse().setTimestamp(block.getBlockHeader().getTimestamp());
    }
    List<org.chainmaker.pb.common.Request.KeyValuePair> parametersList =
        payload.getParametersList();
    org.chainmaker.pb.common.Request.KeyValuePair keyValuePair = parametersList.get(0);
    String proxyInput = keyValuePair.getValue().toStringUtf8();
    if (proxyInput.startsWith(functionEncoder.buildMethodId(FunctionUtility.ProxySendTXMethod))) {
      Tuple3<String, String, byte[]> proxyResult =
          FunctionUtility.getSendTransactionProxyWithoutTxIdFunctionInput(proxyInput);
      resource = proxyResult.getValue2();
      input = Numeric.toHexString(proxyResult.getValue3());
      methodId = input.substring(0, FunctionUtility.MethodIDWithHexPrefixLength);
      if (logger.isDebugEnabled()) {
        logger.debug("  resource: {}, methodId: {}", resource, methodId);
      }
    } else if (proxyInput.startsWith(
        functionEncoder.buildMethodId(FunctionUtility.ProxySendTransactionTXMethod))) {
      Tuple6<String, String, BigInteger, String, String, byte[]> proxyInputResult =
          FunctionUtility.getSendTransactionProxyFunctionInput(proxyInput);
      xaTransactionID = proxyInputResult.getValue2();
      path = proxyInputResult.getValue4();
      resource = Path.decode(path).getResource();
      String methodSig = proxyInputResult.getValue5();
      methodId = functionEncoder.buildMethodId(methodSig);

      if (logger.isDebugEnabled()) {
        logger.debug(
            "xaTransactionID: {}, path: {}, methodSig: {}, methodId: {}",
            xaTransactionID,
            path,
            methodSig,
            methodId);
      }
    } else {
      // transaction not send by proxy
      transaction.setTransactionByProxy(false);
      block.getTransactionsWithDetail().add(transaction);
      return;
    }
    transaction
        .getTransactionRequest()
        .getOptions()
        .put(StubConstant.XA_TRANSACTION_ID, xaTransactionID);
    transaction.setResource(resource);
    // query ABI
    String finalMethodId = methodId;
    String contractAbi = ((ChainMakerConnection) connection).getAbi(resource);
    try {
      ABIDefinition function =
          abiDefinitionFactory.loadABI(contractAbi).getMethodIDToFunctions().get(finalMethodId);
      if (Objects.isNull(function)) {
        logger.warn("Maybe abi is upgraded, Load function failed, methodId: {}", finalMethodId);
      } else {
        transaction.getTransactionRequest().setMethod(function.getName());
      }
    } catch (Exception e) {
      logger.error(
          "Maybe Query abi failed, transactionHash: {},resource:{} MethodId: {},e:",
          txId,
          resource,
          finalMethodId,
          e);
    }
    block.getTransactionsWithDetail().add(transaction);
  }

  @Override
  public void asyncGetTransaction(
      String transactionHash,
      long blockNumber,
      BlockManager blockManager,
      boolean isVerified,
      Connection connection,
      GetTransactionCallback callback) {
    asyncRequestTransactionProof(
        transactionHash,
        connection,
        (exception, proof) -> {
          if (Objects.nonNull(exception)) {
            logger.warn("transactionHash: {} exception: ", transactionHash, exception);
            callback.onResponse(exception, null);
            return;
          }
          if (blockNumber != proof.getTransactionInfo().getBlockHeight()) {
            callback.onResponse(
                new Exception("Transaction hash does not match the block number"), null);
            return;
          }
          if (isVerified) {
            // TODO:  merkle validation need add

          } else {
            assembleTransaction(
                transactionHash, proof.getTransactionInfo(), null, connection, callback);
          }
        });
  }

  private void assembleTransaction(
      String transactionHash,
      ChainmakerTransaction.TransactionInfo transactionInfo,
      Block block,
      Connection connection,
      GetTransactionCallback callback) {
    try {
      String methodId;
      String input;
      String xaTransactionID = "0";
      long xaTransactionSeq = 0;
      String path;
      String resource;
      ChainmakerTransaction.Transaction chainmakerTransaction = transactionInfo.getTransaction();
      org.chainmaker.pb.common.Request.Payload payload = chainmakerTransaction.getPayload();
      String txId = chainmakerTransaction.getPayload().getTxId();
      Transaction transaction = new Transaction();
      // TODO: receipt and txRes
      //      transaction.setReceiptBytes();
      //      transaction.setTxBytes();
      String memberInfo =
          chainmakerTransaction.getSender().getSigner().getMemberInfo().toStringUtf8();
      String hashType =
          connection.getProperties().get(ChainMakerConstant.CHAIN_MAKER_PROPERTY_CRYPTO_HASH);
      String algorithm =
          ((ChainMakerConnection) connection)
              .getClientWrapper()
              .getNativeClient()
              .getClientUser()
              .getPrivateKey()
              .getAlgorithm();
      String userAddr = CryptoUtils.getEVMAddressFromPKPEM(memberInfo, hashType, algorithm);
      transaction.setAccountIdentity(userAddr);
      transaction.setTransactionByProxy(true);
      transaction.getTransactionResponse().setHash(txId);
      if (block != null && block.getBlockHeader() != null) {
        transaction.getTransactionResponse().setTimestamp(block.getBlockHeader().getTimestamp());
      }

      String proxyInput = payload.getParametersList().get(0).getValue().toStringUtf8();
      String proxyOutput = Hex.toHexString(chainmakerTransaction.getResult().toByteArray());
      if (proxyInput.startsWith(functionEncoder.buildMethodId(FunctionUtility.ProxySendTXMethod))) {
        Tuple3<String, String, byte[]> proxyResult =
            FunctionUtility.getSendTransactionProxyWithoutTxIdFunctionInput(proxyInput);
        resource = proxyResult.getValue2();
        input = Numeric.toHexString(proxyResult.getValue3());
        methodId = input.substring(0, FunctionUtility.MethodIDWithHexPrefixLength);
        input = input.substring(FunctionUtility.MethodIDWithHexPrefixLength);

        if (logger.isDebugEnabled()) {
          logger.debug("  resource: {}, methodId: {}", resource, methodId);
        }
      } else if (proxyInput.startsWith(
          functionEncoder.buildMethodId(FunctionUtility.ProxySendTransactionTXMethod))) {
        Tuple6<String, String, BigInteger, String, String, byte[]> proxyInputResult =
            FunctionUtility.getSendTransactionProxyFunctionInput(proxyInput);

        xaTransactionID = proxyInputResult.getValue2();
        xaTransactionSeq = proxyInputResult.getValue3().longValue();
        path = proxyInputResult.getValue4();
        resource = Path.decode(path).getResource();
        String methodSig = proxyInputResult.getValue5();
        input = Numeric.toHexString(proxyInputResult.getValue6());
        methodId = functionEncoder.buildMethodId(methodSig);

        if (logger.isDebugEnabled()) {
          logger.debug(
              "xaTransactionID: {}, xaTransactionSeq: {}, path: {}, methodSig: {}, methodId: {}",
              xaTransactionID,
              xaTransactionSeq,
              path,
              methodSig,
              methodId);
        }
      } else {
        // transaction not send by proxy
        transaction.setTransactionByProxy(false);
        callback.onResponse(null, transaction);
        return;
      }

      transaction
          .getTransactionRequest()
          .getOptions()
          .put(StubConstant.XA_TRANSACTION_ID, xaTransactionID);
      transaction
          .getTransactionRequest()
          .getOptions()
          .put(StubConstant.XA_TRANSACTION_SEQ, xaTransactionSeq);
      transaction.setResource(resource);

      String finalMethodId = methodId;
      String finalInput = input;
      String contractAbi = ((ChainMakerConnection) connection).getAbi(resource);

      ABIDefinition function =
          abiDefinitionFactory.loadABI(contractAbi).getMethodIDToFunctions().get(finalMethodId);

      if (Objects.isNull(function)) {
        logger.warn("Maybe abi is upgraded, Load function failed, methodId: {}", finalMethodId);

        callback.onResponse(null, transaction);
        return;
      }

      ABIObject inputObject = ABIObjectFactory.createInputObject(function);
      List<String> inputParams = codecJsonWrapper.decode(inputObject, finalInput);
      transaction.getTransactionRequest().setMethod(function.getName());
      transaction.getTransactionRequest().setArgs(inputParams.toArray(new String[0]));

      if (transactionInfo.getTransaction().getResult().getCode().getNumber()
          == ResultOuterClass.TxStatusCode.SUCCESS.getNumber()) {
        transaction
            .getTransactionResponse()
            .setMessage(ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));

        ABIObject outputObject = ABIObjectFactory.createOutputObject(function);
        List<String> outputParams =
            codecJsonWrapper.decode(outputObject, proxyOutput.substring(130));
        /** decode output from output */
        transaction.getTransactionResponse().setResult(outputParams.toArray(new String[0]));
        byte[] proxyBytesOutput =
            FunctionUtility.decodeProxyBytesOutput(
                Hex.toHexString(
                    chainmakerTransaction
                        .getResult()
                        .getContractResult()
                        .getResult()
                        .toByteArray()));
        transaction
            .getTransactionResponse()
            .setResult(
                codecJsonWrapper
                    .decode(outputObject, Hex.toHexString(proxyBytesOutput))
                    .toArray(new String[0]));
      }
      transaction
          .getTransactionResponse()
          .setErrorCode(chainmakerTransaction.getResult().getCode().getNumber());
      transaction
          .getTransactionResponse()
          .setMessage(chainmakerTransaction.getResult().getMessage());
      if (logger.isTraceEnabled()) {
        logger.trace("transactionHash: {}, transaction: {}", transactionHash, transaction);
      }

      callback.onResponse(null, transaction);

    } catch (Exception e) {
      logger.warn("transactionHash: {}, exception: ", transactionHash, e);
      callback.onResponse(e, null);
    }
  }

  private interface RequestTransactionProofCallback {
    void onResponse(ChainMakerStubException e, TransactionProof proof);
  }

  private void asyncRequestTransactionProof(
      String transactionHash, Connection connection, RequestTransactionProofCallback callback) {

    Request request =
        Request.newRequest(ChainMakerRequestType.GET_TRANSACTION_PROOF, transactionHash);
    connection.asyncSend(
        request,
        response -> {
          try {
            if (logger.isDebugEnabled()) {
              logger.debug("Request proof, transactionHash: {}", transactionHash);
            }

            if (response.getErrorCode() != ChainMakerStatusCode.Success) {
              callback.onResponse(
                  new ChainMakerStubException(response.getErrorCode(), response.getErrorMessage()),
                  null);
              return;
            }
            // TODO: merkle validation need add
            TransactionProof transactionProof =
                objectMapper.readValue(response.getData(), TransactionProof.class);
            if (logger.isDebugEnabled()) {
              logger.debug(
                  " transactionHash: {}, transactionProof: {}", transactionHash, transactionProof);
            }

            callback.onResponse(null, transactionProof);
          } catch (Exception e) {
            callback.onResponse(
                new ChainMakerStubException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()),
                null);
          }
        });
  }

  @Override
  public void asyncCustomCommand(
      String command,
      Path path,
      Object[] args,
      Account account,
      BlockManager blockManager,
      Connection connection,
      CustomCommandCallback callback) {}

  @Override
  public byte[] accountSign(Account account, byte[] message) {
    if (!(account instanceof ChainMakerAccount)) {
      throw new UnsupportedOperationException(
          "Not ChainMakerAccount, account name: " + account.getClass().getName());
    }
    ChainMakerAccount chainMakerAccount = (ChainMakerAccount) account;
    User user = chainMakerAccount.getUser();
    PrivateKey privateKey = null;
    try {
      byte[] signature = null;
      if (user.getAuthType().equals(AuthType.PermissionedWithCert.getMsg())) {
        if (user.getKeyId() == null || user.getKeyId().equals("")) {
          signature = user.getCryptoSuite().sign(user.getPrivateKey(), message);
        } else {
          signature =
              user.getCryptoSuite()
                  .signWithHsm(Integer.parseInt(user.getKeyId()), user.getKeyType(), message);
        }
      } else {
        privateKey = CryptoUtils.getPrivateKeyFromBytes(user.getPriBytes());
        // TODO: hashType
        String hash = "ECDSA";
        if (chainMakerAccount.getType().equals(ChainMakerConstant.CHAIN_MAKER_GM_EVM_STUB_TYPE)) {
          hash = "SM3";
        }
        signature = user.getCryptoSuite().rsaSign(privateKey, message, hash);
      }
      return signature;
    } catch (Exception e) {
      throw new UnsupportedOperationException(
          "get account user privateKey error,account name:" + account.getClass().getName());
    }
  }

  @Override
  public boolean accountVerify(String identity, byte[] signBytes, byte[] message) {
    return false;
  }

  void asyncCallByProxy(
      TransactionContext context,
      TransactionRequest request,
      Connection connection,
      Callback callback) {
    TransactionResponse transactionResponse = new TransactionResponse();
    try {
      ChainMakerConnection chainMakerConnection = (ChainMakerConnection) connection;
      Map<String, String> properties = chainMakerConnection.getProperties();

      checkProperties(properties);

      String proxyContractAddress =
          chainMakerConnection.getProperty(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME);
      Path path = context.getPath();
      String pathStr = path.toString();
      String name = path.getResource();

      String contractAbi = chainMakerConnection.getAbi(name);
      String contractAddress = chainMakerConnection.getProperty(name);

      if (contractAbi == null) {
        throw new ChainMakerStubException(
            ChainMakerStatusCode.ABINotExist, "resource ABI not exist: " + name);
      }
      // encode
      String[] args = request.getArgs();
      String method = request.getMethod();

      ContractABIDefinition contractABIDefinition = abiDefinitionFactory.loadABI(contractAbi);
      ABIDefinition abiDefinition =
          contractABIDefinition.getFunctions().get(method).stream()
              .filter(function -> function.getInputs().size() == (args == null ? 0 : args.length))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ChainMakerStubException(
                          ChainMakerStatusCode.MethodNotExist, "method not exist: " + method));
      byte[] encodedArgs = Numeric.hexStringToByteArray(encodeFunctionArgs(abiDefinition, args));
      String transactionID = (String) request.getOptions().get(StubConstant.XA_TRANSACTION_ID);

      Function function;
      String proxyMethod;

      if (Objects.isNull(transactionID) || transactionID.isEmpty() || "0".equals(transactionID)) {
        function =
            FunctionUtility.newConstantCallProxyFunction(
                functionEncoder, name, abiDefinition.getMethodSignatureAsString(), encodedArgs);
        proxyMethod = FunctionUtility.ProxyCallMethod;
      } else {
        function =
            FunctionUtility.newConstantCallProxyFunction(
                transactionID, pathStr, abiDefinition.getMethodSignatureAsString(), encodedArgs);
        proxyMethod = FunctionUtility.ProxyCallWithTransactionIdMethod;
      }

      if (logger.isDebugEnabled()) {
        logger.debug(
            " name:{}, address: {}, method: {}, args: {}", name, contractAddress, method, args);
      }

      byte[] encodedMethodWithArgs =
          functionEncoder.encode(function).getBytes(StandardCharsets.UTF_8);

      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, encodedMethodWithArgs);

      TransactionParams transaction =
          new TransactionParams(
              request,
              proxyContractAddress,
              functionEncoder.buildMethodId(proxyMethod),
              params,
              TransactionParams.SUB_TYPE.CALL_BY_PROXY);
      transaction.setAbi(contractAbi);
      Request req =
          Request.newRequest(
              ChainMakerRequestType.CALL, objectMapper.writeValueAsBytes(transaction));
      connection.asyncSend(
          req,
          response -> {
            try {
              if (response.getErrorCode() != ChainMakerStatusCode.Success) {
                throw new ChainMakerStubException(
                    response.getErrorCode(), response.getErrorMessage());
              }
              ResultOuterClass.TxResponse txResponse =
                  ResultOuterClass.TxResponse.parseFrom(response.getData());
              if (logger.isDebugEnabled()) {
                logger.debug(
                    "call result, code: {}, msg: {}",
                    txResponse.getCode(),
                    txResponse.getMessage());
              }
              if (txResponse.getCode().getNumber()
                  == ResultOuterClass.TxStatusCode.SUCCESS.getNumber()) {
                // if success, try to decode results
                transactionResponse.setErrorCode(ChainMakerStatusCode.Success);
                transactionResponse.setMessage(
                    ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
                ABIObject outputObject = ABIObjectFactory.createOutputObject(abiDefinition);
                byte[] proxyBytesOutput =
                    FunctionUtility.decodeProxyBytesOutput(
                        Hex.toHexString(txResponse.getContractResult().getResult().toByteArray()));
                transactionResponse.setResult(
                    codecJsonWrapper
                        .decode(outputObject, Hex.toHexString(proxyBytesOutput))
                        .toArray(new String[0]));
              } else {
                // if error, try to decode revert msg
                transactionResponse.setErrorCode(ChainMakerStatusCode.CallNotSuccessStatus);

                Tuple2<Boolean, String> booleanStringTuple2 =
                    RevertMessage.tryResolveRevertMessage(
                        txResponse.getContractResult().getCode(),
                        txResponse.getContractResult().getMessage());
                if (Boolean.TRUE.equals(booleanStringTuple2.getValue1())) {
                  transactionResponse.setMessage(booleanStringTuple2.getValue2());
                } else {
                  transactionResponse.setMessage(txResponse.getMessage());
                }
              }
              callback.onTransactionResponse(null, transactionResponse);
            } catch (ChainMakerStubException e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(e.getErrorCode(), e.getMessage()), null);
            } catch (Exception e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()),
                  null);
            }
          });

    } catch (ChainMakerStubException e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(e.getErrorCode(), e.getMessage()), null);
    } catch (Exception e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()), null);
    }
  }

  void asyncCallNative(
      TransactionContext context,
      TransactionRequest request,
      Connection connection,
      Callback callback) {
    TransactionResponse transactionResponse = new TransactionResponse();
    try {
      ChainMakerConnection chainMakerConnection = (ChainMakerConnection) connection;
      Map<String, String> properties = chainMakerConnection.getProperties();

      checkProperties(properties);

      Path path = context.getPath();
      String name = path.getResource();

      String contractAbi = chainMakerConnection.getAbi(name);
      String contractAddress = chainMakerConnection.getProperty(name);

      if (contractAbi == null) {
        throw new ChainMakerStubException(
            ChainMakerStatusCode.ABINotExist, "resource ABI not exist: " + name);
      }

      // encode
      String[] args = request.getArgs();
      String method = request.getMethod();

      ContractABIDefinition contractABIDefinition = abiDefinitionFactory.loadABI(contractAbi);
      ABIDefinition abiDefinition =
          contractABIDefinition.getFunctions().get(method).stream()
              .filter(function -> function.getInputs().size() == (args == null ? 0 : args.length))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ChainMakerStubException(
                          ChainMakerStatusCode.MethodNotExist, "method not exist: " + method));

      byte[] encodedMethodWithArgs =
          abiCodec
              .encodeMethodFromString(
                  contractAbi, method, args != null ? Arrays.asList(args) : new ArrayList<>())
              .getBytes(StandardCharsets.UTF_8);

      if (logger.isDebugEnabled()) {
        logger.debug(
            " name:{},address: {}, method: {}, args: {}", name, contractAddress, method, args);
      }

      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, encodedMethodWithArgs);

      TransactionParams transaction =
          new TransactionParams(
              request,
              contractAddress,
              functionEncoder.buildMethodId(abiDefinition.getMethodSignatureAsString()),
              params,
              TransactionParams.SUB_TYPE.CALL);
      transaction.setAbi(contractAbi);
      Request req =
          Request.newRequest(
              ChainMakerRequestType.CALL, objectMapper.writeValueAsBytes(transaction));

      connection.asyncSend(
          req,
          response -> {
            try {
              if (response.getErrorCode() != ChainMakerStatusCode.Success) {
                throw new ChainMakerStubException(
                    response.getErrorCode(), response.getErrorMessage());
              }
              ResultOuterClass.TxResponse txResponse =
                  ResultOuterClass.TxResponse.parseFrom(response.getData());
              if (logger.isDebugEnabled()) {
                logger.debug(
                    "call result, code: {}, msg: {}",
                    txResponse.getCode(),
                    txResponse.getMessage());
              }
              if (txResponse.getCode().getNumber()
                  == ResultOuterClass.TxStatusCode.SUCCESS.getNumber()) {
                // if success, try to decode results
                transactionResponse.setErrorCode(ChainMakerStatusCode.Success);
                transactionResponse.setMessage(
                    ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
                ABIObject outputObject = ABIObjectFactory.createOutputObject(abiDefinition);
                transactionResponse.setResult(
                    codecJsonWrapper
                        .decode(
                            outputObject,
                            Hex.toHexString(
                                txResponse.getContractResult().getResult().toByteArray()))
                        .toArray(new String[0]));
              } else {
                // if error, try to decode revert msg
                transactionResponse.setErrorCode(ChainMakerStatusCode.CallNotSuccessStatus);

                Tuple2<Boolean, String> booleanStringTuple2 =
                    RevertMessage.tryResolveRevertMessage(
                        txResponse.getContractResult().getCode(),
                        txResponse.getContractResult().getMessage());
                if (Boolean.TRUE.equals(booleanStringTuple2.getValue1())) {
                  transactionResponse.setMessage(booleanStringTuple2.getValue2());
                } else {
                  transactionResponse.setMessage(txResponse.getMessage());
                }
              }
              callback.onTransactionResponse(null, transactionResponse);
            } catch (ChainMakerStubException e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(e.getErrorCode(), e.getMessage()), null);
            } catch (Exception e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()),
                  null);
            }
          });
    } catch (ChainMakerStubException e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(e.getErrorCode(), e.getMessage()), null);
    } catch (Exception e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()), null);
    }
  }

  void asyncSendTransactionByProxy(
      TransactionContext context,
      TransactionRequest request,
      Connection connection,
      Callback callback) {
    TransactionResponse transactionResponse = new TransactionResponse();
    try {
      ChainMakerConnection chainMakerConnection = (ChainMakerConnection) connection;
      Map<String, String> properties = chainMakerConnection.getProperties();

      checkProperties(properties);

      String proxyContractAddress =
          chainMakerConnection.getProperty(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME);
      Path path = context.getPath();
      String pathStr = path.toString();
      String name = path.getResource();

      String contractAbi = chainMakerConnection.getAbi(name);
      String contractAddress = chainMakerConnection.getProperty(name);

      if (contractAbi == null) {
        throw new ChainMakerStubException(
            ChainMakerStatusCode.ABINotExist, "resource:" + name + " not exist");
      }

      ChainMakerPublicAccount account = (ChainMakerPublicAccount) context.getAccount();
      User user = account.getUser();

      // encode
      String[] args = request.getArgs();
      String method = request.getMethod();

      ContractABIDefinition contractABIDefinition = abiDefinitionFactory.loadABI(contractAbi);
      ABIDefinition abiDefinition =
          contractABIDefinition.getFunctions().get(method).stream()
              .filter(function -> function.getInputs().size() == (args == null ? 0 : args.length))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ChainMakerStubException(
                          ChainMakerStatusCode.MethodNotExist, "method not exist: " + method));

      byte[] encodedArgs = Numeric.hexStringToByteArray(encodeFunctionArgs(abiDefinition, args));

      String uniqueID = (String) request.getOptions().get(StubConstant.TRANSACTION_UNIQUE_ID);
      String uid =
          Objects.nonNull(uniqueID) ? uniqueID : UUID.randomUUID().toString().replaceAll("-", "");

      String transactionID = (String) request.getOptions().get(StubConstant.XA_TRANSACTION_ID);

      Long transactionSeq = (Long) request.getOptions().get(StubConstant.XA_TRANSACTION_SEQ);
      long seq = Objects.isNull(transactionSeq) ? 0 : transactionSeq;

      Function function;
      String proxyMethod;

      if (Objects.isNull(transactionID) || transactionID.isEmpty() || "0".equals(transactionID)) {
        function =
            FunctionUtility.newSendTransactionProxyFunction(
                functionEncoder,
                uid,
                name,
                abiDefinition.getMethodSignatureAsString(),
                encodedArgs);
        proxyMethod = FunctionUtility.ProxySendTXMethod;
      } else {
        function =
            FunctionUtility.newSendTransactionProxyFunction(
                uid,
                transactionID,
                seq,
                pathStr,
                abiDefinition.getMethodSignatureAsString(),
                encodedArgs);
        proxyMethod = FunctionUtility.ProxySendTransactionTXMethod;
      }

      if (logger.isDebugEnabled()) {
        logger.debug(
            " name:{},address: {}, method: {}, args: {}", name, contractAddress, method, args);
      }

      byte[] encodedMethodWithArgs =
          functionEncoder.encode(function).getBytes(StandardCharsets.UTF_8);

      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, encodedMethodWithArgs);

      TransactionParams transactionParams =
          new TransactionParams(
              request,
              proxyContractAddress,
              functionEncoder.buildMethodId(proxyMethod),
              params,
              TransactionParams.SUB_TYPE.SEND_TX_BY_PROXY);
      transactionParams.setAbi(contractAbi);
      transactionParams.setSignKey(user.getPriBytes());
      Request req =
          Request.newRequest(
              ChainMakerRequestType.SEND_TRANSACTION,
              objectMapper.writeValueAsBytes(transactionParams));
      connection.asyncSend(
          req,
          response -> {
            try {
              if (response.getErrorCode() != ChainMakerStatusCode.Success) {
                throw new ChainMakerStubException(
                    response.getErrorCode(), response.getErrorMessage());
              }
              ResultOuterClass.TxResponse txResponse =
                  ResultOuterClass.TxResponse.parseFrom(response.getData());
              if (logger.isDebugEnabled()) {
                logger.debug(
                    "send transaction result, code: {}, msg: {}",
                    txResponse.getCode(),
                    txResponse.getMessage());
              }
              if (txResponse.getCode().getNumber()
                  == ResultOuterClass.TxStatusCode.SUCCESS.getNumber()) {
                transactionResponse.setErrorCode(ChainMakerStatusCode.Success);
                transactionResponse.setMessage(
                    ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
                ABIObject outputObject = ABIObjectFactory.createOutputObject(abiDefinition);
                byte[] proxyBytesOutput =
                    FunctionUtility.decodeProxyBytesOutput(
                        Hex.toHexString(txResponse.getContractResult().getResult().toByteArray()));
                transactionResponse.setResult(
                    codecJsonWrapper
                        .decode(outputObject, Hex.toHexString(proxyBytesOutput))
                        .toArray(new String[0]));

              } else {
                transactionResponse.setErrorCode(
                    ChainMakerStatusCode.SendTransactionNotSuccessStatus);
                Tuple2<Boolean, String> booleanStringTuple2 =
                    RevertMessage.tryResolveRevertMessage(
                        txResponse.getContractResult().getCode(),
                        txResponse.getContractResult().getMessage());
                if (Boolean.TRUE.equals(booleanStringTuple2.getValue1())) {
                  transactionResponse.setMessage(booleanStringTuple2.getValue2());
                } else {
                  transactionResponse.setMessage(txResponse.getMessage());
                }
              }
              callback.onTransactionResponse(null, transactionResponse);
            } catch (ChainMakerStubException e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(e.getErrorCode(), e.getMessage()), null);
            } catch (Exception e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()),
                  null);
            }
          });
    } catch (ChainMakerStubException e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(e.getErrorCode(), e.getMessage()), null);
    } catch (Exception e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()), null);
    }
  }

  void asyncSendTransactionNative(
      TransactionContext context,
      TransactionRequest request,
      Connection connection,
      Callback callback) {
    TransactionResponse transactionResponse = new TransactionResponse();
    try {
      ChainMakerConnection chainMakerConnection = (ChainMakerConnection) connection;
      Map<String, String> properties = chainMakerConnection.getProperties();

      checkProperties(properties);

      Path path = context.getPath();
      String name = path.getResource();

      String contractAbi = chainMakerConnection.getAbi(name);
      String contractAddress = chainMakerConnection.getProperty(name);

      if (contractAbi == null) {
        throw new ChainMakerStubException(
            ChainMakerStatusCode.ABINotExist, "resource ABI not exist: " + name);
      }

      ChainMakerPublicAccount account = (ChainMakerPublicAccount) context.getAccount();
      User user = account.getUser();

      // encode
      String[] args = request.getArgs();
      String method = request.getMethod();

      ContractABIDefinition contractABIDefinition = abiDefinitionFactory.loadABI(contractAbi);
      ABIDefinition abiDefinition =
          contractABIDefinition.getFunctions().get(method).stream()
              .filter(function -> function.getInputs().size() == (args == null ? 0 : args.length))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ChainMakerStubException(
                          ChainMakerStatusCode.MethodNotExist, "method not exist: " + method));

      byte[] encodedMethodWithArgs =
          abiCodec
              .encodeMethodFromString(
                  contractAbi, method, args != null ? Arrays.asList(args) : new ArrayList<>())
              .getBytes(StandardCharsets.UTF_8);

      if (logger.isDebugEnabled()) {
        logger.debug(
            " name:{},address: {}, method: {}, args: {}", name, contractAddress, method, args);
      }

      Map<String, byte[]> params = new HashMap<>();
      params.put(ChainMakerConstant.CHAIN_MAKER_CONTRACT_ARGS_EVM_PARAM, encodedMethodWithArgs);
      TransactionParams transactionParams =
          new TransactionParams(
              request,
              contractAddress,
              functionEncoder.buildMethodId(abiDefinition.getMethodSignatureAsString()),
              params,
              TransactionParams.SUB_TYPE.SEND_TX);
      transactionParams.setAbi(contractAbi);
      transactionParams.setSignKey(user.getPriBytes());
      Request req =
          Request.newRequest(
              ChainMakerRequestType.SEND_TRANSACTION,
              objectMapper.writeValueAsBytes(transactionParams));
      connection.asyncSend(
          req,
          response -> {
            try {
              if (response.getErrorCode() != ChainMakerStatusCode.Success) {
                throw new ChainMakerStubException(
                    response.getErrorCode(), response.getErrorMessage());
              }
              ResultOuterClass.TxResponse txResponse =
                  ResultOuterClass.TxResponse.parseFrom(response.getData());
              if (logger.isDebugEnabled()) {
                logger.debug(
                    "call result, code: {}, msg: {}",
                    txResponse.getCode(),
                    txResponse.getMessage());
              }
              if (txResponse.getCode().getNumber()
                  == ResultOuterClass.TxStatusCode.SUCCESS.getNumber()) {
                // if success, try to decode results
                transactionResponse.setErrorCode(ChainMakerStatusCode.Success);
                transactionResponse.setMessage(
                    ChainMakerStatusCode.getStatusMessage(ChainMakerStatusCode.Success));
                ABIObject outputObject = ABIObjectFactory.createOutputObject(abiDefinition);
                transactionResponse.setResult(
                    codecJsonWrapper
                        .decode(
                            outputObject,
                            Hex.toHexString(
                                txResponse.getContractResult().getResult().toByteArray()))
                        .toArray(new String[0]));
              } else {
                // if error, try to decode revert msg
                transactionResponse.setErrorCode(ChainMakerStatusCode.CallNotSuccessStatus);
                Tuple2<Boolean, String> booleanStringTuple2 =
                    RevertMessage.tryResolveRevertMessage(
                        txResponse.getContractResult().getCode(),
                        txResponse.getContractResult().getMessage());
                if (Boolean.TRUE.equals(booleanStringTuple2.getValue1())) {
                  transactionResponse.setMessage(booleanStringTuple2.getValue2());
                } else {
                  transactionResponse.setMessage(txResponse.getMessage());
                }
              }
              callback.onTransactionResponse(null, transactionResponse);
            } catch (ChainMakerStubException e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(e.getErrorCode(), e.getMessage()), null);
            } catch (Exception e) {
              logger.warn(" e: ", e);
              callback.onTransactionResponse(
                  new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()),
                  null);
            }
          });
    } catch (ChainMakerStubException e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(e.getErrorCode(), e.getMessage()), null);
    } catch (Exception e) {
      logger.warn(" e: ", e);
      callback.onTransactionResponse(
          new TransactionException(ChainMakerStatusCode.UnclassifiedError, e.getMessage()), null);
    }
  }

  public void checkProperties(Map<String, String> properties) throws ChainMakerStubException {
    if (!properties.containsKey(ChainMakerConstant.CHAIN_MAKER_PROXY_NAME)) {
      throw new ChainMakerStubException(
          ChainMakerStatusCode.InvalidParameter,
          "Proxy contract address not found, resource: "
              + ChainMakerConstant.CHAIN_MAKER_PROXY_NAME);
    }

    if (!properties.containsKey(ChainMakerConstant.CHAIN_MAKER_PROPERTY_CHAIN_ID)) {
      throw new ChainMakerStubException(
          ChainMakerStatusCode.InvalidParameter,
          "Chain id not found, resource: " + ChainMakerConstant.CHAIN_MAKER_PROPERTY_CHAIN_ID);
    }
  }

  private String encodeFunctionArgs(ABIDefinition abiFunction, String[] args) throws IOException {
    ABIObject inputObj = ABIObjectFactory.createInputObject(abiFunction);
    ABIObject encodeObj =
        codecJsonWrapper.encode(inputObj, args != null ? Arrays.asList(args) : new ArrayList<>());
    return encodeObj.encode();
  }
}
