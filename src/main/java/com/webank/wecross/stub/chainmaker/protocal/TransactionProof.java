package com.webank.wecross.stub.chainmaker.protocal;

import org.chainmaker.pb.common.ChainmakerTransaction;

public class TransactionProof {
  // TODO:  merkle validation need add
  private ChainmakerTransaction.TransactionInfo transactionInfo;

  public ChainmakerTransaction.TransactionInfo getTransactionInfo() {
    return transactionInfo;
  }

  public void setTransactionInfo(ChainmakerTransaction.TransactionInfo transactionInfo) {
    this.transactionInfo = transactionInfo;
  }
}
