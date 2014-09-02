package org.redpill_linpro.alfresco.repo.bean;

import java.io.Serializable;

public class AdminReplaceUserRequestBean implements Serializable {
  private static final long serialVersionUID = 847733470506552248L;
  private String content;
  private Boolean disableUsers;
  private Boolean transferSiteMemberships;
  private Boolean transferFileOwnerships;
  private Boolean test;

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Boolean getDisableUsers() {
    return disableUsers;
  }

  public void setDisableUsers(Boolean disableUsers) {
    this.disableUsers = disableUsers;
  }

  public Boolean getTransferSiteMemberships() {
    return transferSiteMemberships;
  }

  public void setTransferSiteMemberships(Boolean transferSiteMemberships) {
    this.transferSiteMemberships = transferSiteMemberships;
  }

  public Boolean getTransferFileOwnerships() {
    return transferFileOwnerships;
  }

  public void setTransferFileOwnerships(Boolean transferFileOwnerships) {
    this.transferFileOwnerships = transferFileOwnerships;
  }

  public Boolean getTest() {
    return test;
  }

  public void setTest(Boolean test) {
    this.test = test;
  }
}
