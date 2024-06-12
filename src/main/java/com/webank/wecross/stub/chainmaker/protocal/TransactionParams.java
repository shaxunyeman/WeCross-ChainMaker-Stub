package com.webank.wecross.stub.chainmaker.protocal;

import com.webank.wecross.stub.TransactionRequest;
import java.util.Map;
import org.chainmaker.sdk.User;

public class TransactionParams {
  private TransactionRequest transactionRequest;
  private String contractAddress;
  private String contractMethodId;
  private Map<String, byte[]> contractMethodParams;
  private SUB_TYPE subType;

  // target contract abi
  private String abi;

  // send transaction signature key
  private byte[] signKey;

  private User user;

  public enum SUB_TYPE {
    SEND_TX_BY_PROXY,
    CALL_BY_PROXY,
    SEND_TX,
    CALL
  }

  public TransactionParams() {}

  public TransactionParams(
      TransactionRequest transactionRequest,
      String contractAddress,
      String contractMethodId,
      Map<String, byte[]> contractMethodParams,
      SUB_TYPE type) {
    this.transactionRequest = transactionRequest;
    this.contractAddress = contractAddress;
    this.contractMethodId = contractMethodId;
    this.contractMethodParams = contractMethodParams;
    this.subType = type;
  }

  public TransactionRequest getTransactionRequest() {
    return transactionRequest;
  }

  public void setTransactionRequest(TransactionRequest transactionRequest) {
    this.transactionRequest = transactionRequest;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
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

  public byte[] getSignKey() {
    return signKey;
  }

  public void setSignKey(byte[] signKey) {
    this.signKey = signKey;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public String toString() {
    return "TransactionParams{"
        + "abi='"
        + abi
        + '\''
        + ", subType="
        + subType
        + ", contractMethodId='"
        + contractMethodId
        + '\''
        + ", contractAddress='"
        + contractAddress
        + '\''
        + ", transactionRequest="
        + transactionRequest
        + '}';
  }
}
