package com.webank.wecross.stub.chainmaker.utils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ABIContentUtility {
  private static final String EVM_CONTRACT_PATH = "contracts" + File.separator + "evm";

  private static File EVMContractPath(String rootPath, String contractName) throws Exception {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource resource = resolver.getResource(rootPath);
    File path =
        new File(
            resource.getFile().getPath()
                + File.separator
                + EVM_CONTRACT_PATH
                + File.separator
                + contractName);
    if (!path.exists()) {
      path.mkdirs();
    }
    return path;
  }

  public static void writeContractABI(String rootPath, String contractName, String abiContent)
      throws Exception {
    File filePath = EVMContractPath(rootPath, contractName);
    if (!filePath.exists()) {
      throw new Exception(String.format("%s not exists.", filePath.getPath()));
    }

    File abiFile = new File(filePath.getPath() + File.separator + contractName + ".abi");
    FileWriter fw = new FileWriter(abiFile);
    fw.write(abiContent);
    fw.close();
  }

  public static String readContractABI(String rootPath, String contractName) throws Exception {
    File abiFilePath =
        new File(
            EVMContractPath(rootPath, contractName).getPath()
                + File.separator
                + contractName
                + ".abi");
    if (!abiFilePath.exists()) {
      throw new Exception(String.format("%s not exists.", abiFilePath.getPath()));
    }

    return new String(Files.readAllBytes(Paths.get(abiFilePath.getPath())));
  }

  public static List<String> listContractNames(String rootPath) throws Exception {
    List<String> contractNames = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource resource = resolver.getResource(rootPath);
    File path = new File(resource.getFile().getPath() + File.separator + EVM_CONTRACT_PATH);
    for (File sub : path.listFiles()) {
      if (sub.isHidden()) {
        continue;
      }
      if (sub.isFile()) {
        continue;
      }
      contractNames.add(sub.getName());
    }
    return contractNames;
  }
}
