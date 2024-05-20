package com.webank.wecross.stub.chainmaker.config;

import com.moandjiezana.toml.Toml;
import com.webank.wecross.stub.chainmaker.common.ChainMakerToml;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainMakerAccountConfigParser extends AbstractChainMakerConfigParser {

  private static final Logger logger = LoggerFactory.getLogger(ChainMakerAccountConfigParser.class);

  public ChainMakerAccountConfigParser(String configPath) {
    super(configPath);
  }

  public ChainMakerAccountConfig loadConfig() throws IOException {
    ChainMakerToml chainMakerToml = new ChainMakerToml(getConfigPath());
    Toml toml = chainMakerToml.getToml();
    return toml.to(ChainMakerAccountConfig.class);
  }
}
