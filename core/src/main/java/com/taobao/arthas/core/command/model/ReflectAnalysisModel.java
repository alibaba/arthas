package com.taobao.arthas.core.command.model;

/**
 * @author: Ares
 * @time: 2023-12-20 11:33:32
 * @description: ReflectAnalysis model
 * @version: JDK 1.8
 */
public class ReflectAnalysisModel extends ResultModel {

  private String refName;

  private String generatedMethodAccessorNames;

  private Integer generatedMethodAccessorNameCount;

  @Override
  public String getType() {
    return "reflect-analysis";
  }

  public String getRefName() {
    return refName;
  }

  public void setRefName(String refName) {
    this.refName = refName;
  }

  public String getGeneratedMethodAccessorNames() {
    return generatedMethodAccessorNames;
  }

  public void setGeneratedMethodAccessorNames(String generatedMethodAccessorNames) {
    this.generatedMethodAccessorNames = generatedMethodAccessorNames;
  }

  public Integer getGeneratedMethodAccessorNameCount() {
    return generatedMethodAccessorNameCount;
  }

  public void setGeneratedMethodAccessorNameCount(Integer generatedMethodAccessorNameCount) {
    this.generatedMethodAccessorNameCount = generatedMethodAccessorNameCount;
  }

}
