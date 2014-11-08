package org.redpill_linpro.alfresco.repo.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.redpill_linpro.alfresco.repo.bean.AdminReplaceUserRequestBean;


/**
 * 
 * Migrate users from one username to another.
 * 
 * @author Marcus Svensson Redpill Linpro AB
 *         <marcus.svensson@redpill-linpro.com>
 *         
 * @author Erik Billerby Redpill Linpro AB
 * 
 */
public interface ReplaceUserService {
  
  public List<Map<String, Serializable>> processUserList(List<Map<String, Serializable>> userList, AdminReplaceUserRequestBean replaceUserBean);


}
