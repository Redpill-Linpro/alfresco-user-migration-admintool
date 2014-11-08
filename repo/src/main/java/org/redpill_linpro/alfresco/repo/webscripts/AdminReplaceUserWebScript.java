package org.redpill_linpro.alfresco.repo.webscripts;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.redpill_linpro.alfresco.repo.bean.AdminReplaceUserRequestBean;
import org.redpill_linpro.alfresco.repo.service.ReplaceUserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * Webscript for handling user replacements in the admin console
 * 
 * @author Marcus Svensson Redpill Linpro AB
 *         <marcus.svensson@redpill-linpro.com>
 *         
 * @author Erik Billerby Redpill Linpro AB
 * 
 */
public class AdminReplaceUserWebScript extends DeclarativeWebScript implements InitializingBean {
  private static final Logger LOG = Logger.getLogger(AdminReplaceUserWebScript.class);

  private PersonService personService;
  private NodeService nodeService;
  private ReplaceUserService replaceUserService;

  protected static final String REGEX_VALIDATION_FORMAT = "(.+,.+)(\\n(.+,.+))*";
  protected static final String SOURCE_USERNAME = "sourceUsername";
  protected static final String SOURCE_FULL_NAME = "sourceFullName";
  protected static final String SOURCE_EMAIL = "sourceEmail";
  protected static final String TARGET_USERNAME = "targetUsername";
  protected static final String TARGET_FULL_NAME = "targetFullName";
  protected static final String TARGET_EMAIL = "targetEmail";
  protected static final String DISABLE_USER = "disableUser";
  protected static final String CHANGE_OWNERSHIP_COUNT = "changeOwnershipCount";
  protected static final String CHANGE_SITE_MEMBERSHIP_COUNT = "changeSiteMembershipCount";
  protected static final String CHANGE_GLOBAL_GROUP_COUNT = "changeGlobalGroupCount";
  protected static final int MAX_RESULTS = 500;

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(AdminReplaceUserWebScript.class.getName() + ".executeImpl");
    }

    Map<String, Object> result = new HashMap<String, Object>();
    //
    AdminReplaceUserRequestBean requestData = null;
    try {
      String jsonString = req.getContent().getContent();
      if (LOG.isDebugEnabled()) {
        LOG.debug("jsonString request string: " + jsonString);
      }
      Gson gson = new Gson();
      Type type = new TypeToken<AdminReplaceUserRequestBean>() {
      }.getType();

      requestData = gson.fromJson(jsonString, type);

    } catch (IOException e) {
      throw new AlfrescoRuntimeException("Failed to parse request body", e);
    }

    if (!validateInputFormat(requestData)) {
      status.setCode(400);
      status.setMessage("Invalid input format");
      status.setRedirect(true);
      return null;
    }

    List<Map<String, Serializable>> resultList = getUserList(requestData);

    resultList = replaceUserService.processUserList(resultList, requestData);

    result.put("items", resultList);

    if (LOG.isTraceEnabled()) {
      LOG.trace(AdminReplaceUserWebScript.class.getName() + ".executeImpl returns " + result.toString());
    }
    return result;
  }



  /**
   * Get a list of users based on the request data
   * 
   * @param requestData
   *          The request data
   * @return a list of users and some supplementary information
   */
  protected List<Map<String, Serializable>> getUserList(AdminReplaceUserRequestBean requestData) {
    final List<Map<String, Serializable>> resultList = new ArrayList<Map<String, Serializable>>();
    String content = requestData.getContent().trim();
    String[] rows = content.split("\\n");
    for (String row : rows) {
      String[] usernames = row.split(",");
      String sourceUsername = usernames[0].trim();
      if ("admin".equalsIgnoreCase(sourceUsername) || "system".equalsIgnoreCase(sourceUsername)) {
        LOG.warn("Skipped transfer of ownership for user " + sourceUsername);
        continue;
      }

      NodeRef sourcePersonNodeRef = personService.getPersonOrNull(sourceUsername);
      if (sourcePersonNodeRef == null) {
        LOG.warn("skipping " + sourceUsername + ": could not find user");
        continue;
      }
      String sourceFullName = (String) nodeService.getProperty(sourcePersonNodeRef, ContentModel.PROP_FIRSTNAME);
      sourceFullName = sourceFullName + " " + (String) nodeService.getProperty(sourcePersonNodeRef, ContentModel.PROP_LASTNAME);
      String sourceEmail = (String) nodeService.getProperty(sourcePersonNodeRef, ContentModel.PROP_EMAIL);

      String targetUsername = usernames[1].trim();
      NodeRef targetPersonNodeRef = personService.getPersonOrNull(targetUsername);
      if (targetPersonNodeRef == null) {
        LOG.warn("skipping " + targetUsername + ": could not find user");
        continue;
      }

      // Make sure we dont try transfer membership to the same username
      if (sourceUsername.equalsIgnoreCase(targetUsername)) {
        LOG.warn("skipping " + targetUsername + ": same username for source and target specified");
        continue;
      }
      String targetFullName = (String) nodeService.getProperty(targetPersonNodeRef, ContentModel.PROP_FIRSTNAME);
      targetFullName = targetFullName + " " + (String) nodeService.getProperty(targetPersonNodeRef, ContentModel.PROP_LASTNAME);
      String targetEmail = (String) nodeService.getProperty(targetPersonNodeRef, ContentModel.PROP_EMAIL);

      Map<String, Serializable> entry = new HashMap<String, Serializable>();
      entry.put(SOURCE_USERNAME, sourceUsername);
      entry.put(SOURCE_FULL_NAME, sourceFullName);
      entry.put(SOURCE_EMAIL, sourceEmail);
      entry.put(TARGET_USERNAME, targetUsername);
      entry.put(TARGET_FULL_NAME, targetFullName);
      entry.put(TARGET_EMAIL, targetEmail);

      resultList.add(entry);
    }
    return resultList;
  }


  /**
   * Validate that the input list is valid
   * 
   * @param requestData
   * @return
   */
  protected boolean validateInputFormat(AdminReplaceUserRequestBean requestData) {
    String content = requestData.getContent();
    return content != null && content.length() > 0 && content.trim().matches(REGEX_VALIDATION_FORMAT);
  }

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setReplaceUserService(ReplaceUserService replaceUserService) {
    this.replaceUserService = replaceUserService;
  }
  
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(personService);
    Assert.notNull(nodeService);
    Assert.notNull(replaceUserService);
    
  }

}
