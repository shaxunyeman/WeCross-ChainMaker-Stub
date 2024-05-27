package com.webank.wecross.stub.chainmaker.utils;

import com.webank.wecross.stub.Block;
import com.webank.wecross.stub.BlockHeader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.chainmaker.pb.common.ChainmakerBlock;
import org.chainmaker.pb.common.ChainmakerTransaction;
import org.fisco.bcos.sdk.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUtility {

  private static final Logger logger = LoggerFactory.getLogger(BlockUtility.class);

  /**
   * convert chainMaker Block to BlockHeader
   *
   * @param chainmakerBlock
   * @return
   */
  public static BlockHeader convertToBlockHeader(ChainmakerBlock.Block chainmakerBlock) {
    ChainmakerBlock.BlockHeader chainMakerHeader = chainmakerBlock.getHeader();
    BlockHeader blockHeader = new BlockHeader();
    String blockHash = Hex.toHexString(chainMakerHeader.getBlockHash().toByteArray());
    blockHeader.setHash(blockHash);
    String prevHash =
        ObjectUtils.isEmpty(chainMakerHeader.getPreBlockHash().toByteArray())
            ? null
            : Hex.toHexString(chainMakerHeader.getPreBlockHash().toByteArray());
    blockHeader.setPrevHash(prevHash);
    blockHeader.setNumber(chainMakerHeader.getBlockHeight());
    // TransactionRoot
    String txRoot = Hex.toHexString(chainMakerHeader.getTxRoot().toByteArray());
    blockHeader.setTransactionRoot(txRoot);
    blockHeader.setTimestamp(chainMakerHeader.getBlockTimestamp());
    return blockHeader;
  }

  /**
   * convert chainMaker Tx to TxHashes
   *
   * @param chainmakerBlock
   * @param onlyHeader
   * @return
   * @throws IOException
   */
  public static Block convertToBlock(ChainmakerBlock.Block chainmakerBlock, boolean onlyHeader) {
    Block stubBlock = new Block();
    BlockHeader blockHeader = convertToBlockHeader(chainmakerBlock);
    stubBlock.setBlockHeader(blockHeader);
    List<String> txs = new ArrayList<>();
    if (!onlyHeader && chainmakerBlock.getTxsCount() > 0) {
      for (int i = 0; i < chainmakerBlock.getTxsList().size(); i++) {
        ChainmakerTransaction.Transaction transaction = chainmakerBlock.getTxsList().get(i);
        txs.add(transaction.getPayload().getTxId());
      }
    }
    stubBlock.setTransactionsHashes(txs);
    return stubBlock;
  }

  public static Block convertToBlock(ChainmakerBlock.BlockInfo blockInfo, boolean onlyHeader) {
    ChainmakerBlock.Block chainmakerBlock = blockInfo.getBlock();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "blockNumber: {}, blockHash: {}",
          chainmakerBlock.getHeader().getBlockHeight(),
          chainmakerBlock.getHeader().getBlockHash());
    }
    Block stubBlock = convertToBlock(chainmakerBlock, onlyHeader);
    stubBlock.setRawBytes(blockInfo.toByteArray());
    return stubBlock;
  }
}
