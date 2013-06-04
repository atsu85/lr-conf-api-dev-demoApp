package org.zeroturnaround.liverebel;

import org.zeroturnaround.liverebel.example.conf.LocalFileConfigurationResolver;

public class NoLiveRebelConfigurationResolverImpl extends LocalFileConfigurationResolver {

  @Override
  public String getRuntimeConfFilePath() {
    return "/tmp/managed.runtime.properties";
  }
}
