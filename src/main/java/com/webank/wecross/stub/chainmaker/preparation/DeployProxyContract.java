package com.webank.wecross.stub.chainmaker.preparation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployProxyContract {
  private static final Logger logger = LoggerFactory.getLogger(DeployProxyContract.class);

  public static String getUsage(String chainPath) {
    String pureChainPath = chainPath.replace("classpath:/", "").replace("classpath:", "");
    return "Usage:\n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " deploy [chainName] [accountName(optional)]\n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " upgrade [chainName] [accountName(optional)]\n"
        + "Example:\n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " deploy "
        + pureChainPath
        + " \n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " deploy "
        + pureChainPath
        + " admin\n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " upgrade "
        + pureChainPath
        + " \n"
        + "         java -cp 'conf/:lib/*:plugin/*' "
        + DeployProxyContract.class.getName()
        + " upgrade "
        + pureChainPath
        + " admin";
  }

  public static void main(String[] args) {
    try {
      switch (args.length) {
        case 2:
          handle2Args(args);
          break;
        default:
          usage();
      }
    } catch (Exception e) {
      System.out.println("Failed, please check account or contract. " + e);
      logger.warn("Error: ", e);
    } finally {
      exit();
    }
  }

  private static void usage() {
    System.out.println(getUsage("chains/chaimaker"));
    exit();
  }

  private static void exit() {
    System.exit(0);
  }

  private static void handle2Args(String[] args) throws Exception {
    if (args.length != 2) {
      usage();
    }

    String cmd = args[0];
    String chainPath = args[1];

    switch (cmd) {
      case "deploy":
        deploy(chainPath);
        break;
      case "upgrade":
        upgrade(chainPath);
        break;
      default:
        usage();
    }
  }

  private static void deploy(String chainPath) throws Exception {
    DeployWeCrossContract weCross = DeployWeCrossContract.build(chainPath, "stub.toml");
    weCross.deployWeCrossProxy(true);
  }

  private static void upgrade(String chainPath) throws Exception {
    DeployWeCrossContract weCross = DeployWeCrossContract.build(chainPath, "stub.toml");
    weCross.deployWeCrossProxy(false);
  }
}
