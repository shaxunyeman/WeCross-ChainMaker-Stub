package com.webank.wecross.stub.chainmaker.protocal;

import com.webank.wecross.stub.TransactionRequest;
import java.util.Map;

public class TransactionParams {
  private TransactionRequest transactionRequest;
  private String contractCallName;
  private String contractMethodId;
  private Map<String, byte[]> contractMethodParams;
  private byte[] signData;
  private SUB_TYPE subType;
  private String abi;

  public enum SUB_TYPE {
    SEND_TX_BY_PROXY,
    CALL_BY_PROXY,
    SEND_TX,
    CALL
  }

  public TransactionParams(
      TransactionRequest transactionRequest,
      String contractCallName,
      String contractMethodId,
      Map<String, byte[]> contractMethodParams,
      SUB_TYPE type) {
    this.transactionRequest = transactionRequest;
    this.contractCallName = contractCallName;
    this.contractMethodId = contractMethodId;
    this.contractMethodParams = contractMethodParams;
    this.subType = type;
  }

  public TransactionParams(TransactionRequest transactionRequest, byte[] signData, SUB_TYPE type) {
    this.transactionRequest = transactionRequest;
    this.signData = signData;
    this.subType = type;
  }

  public TransactionRequest getTransactionRequest() {
    return transactionRequest;
  }

  public void setTransactionRequest(TransactionRequest transactionRequest) {
    this.transactionRequest = transactionRequest;
  }

  public String getContractCallName() {
    return contractCallName;
  }

  public void setContractCallName(String contractCallName) {
    this.contractCallName = contractCallName;
  }

  public String getContractMethodId() {
    return contractMethodId;
  }

  public void setContractMethodId(String contractMethodId) {
    this.contractMethodId = contractMethodId;
  }

  public Map<String, byte[]> getContractMethodParams() {
    return contractMethodParams;
  }

  public void setContractMethodParams(Map<String, byte[]> contractMethodParams) {
    this.contractMethodParams = contractMethodParams;
  }

  public byte[] getSignData() {
    return signData;
  }

  public SUB_TYPE getSubType() {
    return subType;
  }

  public void setSubType(SUB_TYPE subType) {
    this.subType = subType;
  }

  public String getAbi() {
    return abi;
  }

  public void setAbi(String abi) {
    this.abi = abi;
  }

  @Override
  public String toString() {
    return "TransactionParams{"
        + "transactionRequest="
        + transactionRequest
        + ", contractName='"
        + contractCallName
        + '\''
        + ", contractMethod='"
        + contractMethodId
        + '\''
        + ", contractMethodParams="
        + contractMethodParams
        + ", subType="
        + subType
        + '}';
  }
}
