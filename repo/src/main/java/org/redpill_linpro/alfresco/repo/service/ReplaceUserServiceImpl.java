package org.redpill_linpro.alfresco.repo.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.Element;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.redpill_linpro.alfresco.repo.bean.AdminReplaceUserRequestBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ReplaceUserServiceImpl implements ReplaceUserService, InitializingBean {

  private static final Logger LOG = Logger.getLogger(ReplaceUserServiceImpl.class);

  private PersonService personService;
  private NodeService nodeService;
  private SearchService searchService;
  private SiteService siteService;
  private OwnableService ownableService;
  private BehaviourFilter behaviourFilter;
  private MutableAuthenticationService authenticationService;
  private PermissionService permissionService;
  private AuthorityService authorityService;

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
  public List<Map<String, Serializable>> processUserList(List<Map<String, Serializable>> resultList, AdminReplaceUserRequestBean replaceUserBean) {
    // Disable users
    if (replaceUserBean.getDisableUsers()) {
      disableUsers(resultList, replaceUserBean.getTest());
    }

    // Transfer file ownerships
    if (replaceUserBean.getTransferFileOwnerships()) {
      transferOwnership(resultList, replaceUserBean.getTest());
    }

    // Transfer site memberships
    if (replaceUserBean.getTransferSiteMemberships()) {
      transferMemberships(resultList, replaceUserBean.getTest());
    }

    // Transfer global groups
    if (replaceUserBean.getTransferGlobalGroups()) {
      transferGlobalGroupMemberships(resultList, replaceUserBean.getTest());
    }
    
    return resultList;

  }  
  private List<Map<String, Serializable>> disableUsers(List<Map<String, Serializable>> userList, Boolean test) {
    for (Map<String, Serializable> user : userList) {
      String sourceUsername = (String) user.get(SOURCE_USERNAME);
      if (personService.isEnabled(sourceUsername)) {
        user.put(DISABLE_USER, "1");
        if (!test) {
          LOG.info("Disabling user " + sourceUsername);
          authenticationService.setAuthenticationEnabled(sourceUsername, false);
        }
      } else {
        LOG.info("User " + sourceUsername + " already disabled");
      }
    }
    return userList;

  }
  /**
   * Transfer ownership of files from one user to another
   * 
   * @param userList
   *          The list of users
   * @param test
   *          If set to true no modification of data will be made, if set to
   *          false file ownerships will be made
   * @return
   */
  protected List<Map<String, Serializable>> transferOwnership(List<Map<String, Serializable>> userList, Boolean test) {
    // Disable behaviours for this script
    behaviourFilter.disableBehaviour();
    for (Map<String, Serializable> user : userList) {
      int skip = 0;
      String sourceUser = (String) user.get(SOURCE_USERNAME);
      String targetUser = (String) user.get(TARGET_USERNAME);
      // String query =
      // "(TYPE:\"cm:content\" OR TYPE:\"cm:folder\") AND PATH:\"/app:company_home//*\" AND (cm:owner:"
      // + sourceUser +" OR cm:creator:" + sourceUser +") ";

      String query = "(TYPE:\"cm:content\" OR TYPE:\"cm:folder\") AND PATH:\"/app:company_home//*\" AND (cm:owner:\"" + sourceUser + "\" OR (ISNULL:\"cm:owner\" AND cm:creator:\"" + sourceUser + "\"))";

      SearchParameters searchParameters = new SearchParameters();
      searchParameters.setQuery(query);
      searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
      searchParameters.setMaxItems(MAX_RESULTS);
      searchParameters.setSkipCount(skip);
      searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

      ResultSet result = searchService.query(searchParameters);
      List<NodeRef> resultNodeRefs = result.getNodeRefs();
      List<NodeRef> allNodeRefs = new ArrayList<NodeRef>();
      allNodeRefs.addAll(resultNodeRefs);
      LOG.debug("Adding first " + resultNodeRefs.size() + " objects to list (Limit is " + MAX_RESULTS + ")");
      while (resultNodeRefs.size() == MAX_RESULTS) {
        skip = skip + MAX_RESULTS;
        searchParameters.setSkipCount(skip);
        result = searchService.query(searchParameters);
        resultNodeRefs = result.getNodeRefs();
        LOG.debug("Adding another " + resultNodeRefs.size() + " objects to list");
        allNodeRefs.addAll(resultNodeRefs);

        if (skip >= 50000) {
          throw new AlfrescoRuntimeException("Something is wrong, too many files!");
        }
      }

      LOG.info("Actual number of objects is  " + allNodeRefs.size() + " for user " + sourceUser);
      List<NodeRef> filteredNodeRefs = new ArrayList<NodeRef>();
      for (NodeRef nodeRef : allNodeRefs) {
        Path path = nodeService.getPath(nodeRef);

        if (path.size() > 4) {
          Element element = path.get(4);
          Element element2 = path.get(3);
          String matchingName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}surf-config";
          if (matchingName.equalsIgnoreCase(element.getElementString()) || matchingName.equalsIgnoreCase(element2.getElementString())) {
            LOG.trace("Skipping surf-config for " + nodeRef.toString());
            continue;
          }
        }
        if (LOG.isDebugEnabled()) {
          String displayPath = path.toDisplayPath(nodeService, permissionService) + "/" + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
          LOG.debug("Path: " + displayPath);
        }
        filteredNodeRefs.add(nodeRef);
      }
      if (!test) {
        LOG.info("Changing ownership on " + filteredNodeRefs.size() + " objects: " + sourceUser + "->" + targetUser);
        for (NodeRef nodeRef : filteredNodeRefs) {
          ownableService.setOwner(nodeRef, targetUser);
        }
      }

      user.put(CHANGE_OWNERSHIP_COUNT, filteredNodeRefs.size());

    }
    behaviourFilter.enableBehaviour();
    return userList;
  }

  /**
   * Transfer site memberships from one person to another Does not take group
   * membership into account. If the source user was a member of a group, then
   * the new user will just be a member of the site. This will overwrite any
   * previous role of a user in the site.
   * 
   * @param userList
   *          List of users
   * @param test
   *          If set to true no modification of data will be made, if set to
   *          false file ownerships will be made
   * @return
   */
  protected List<Map<String, Serializable>> transferMemberships(List<Map<String, Serializable>> userList, Boolean test) {

    for (Map<String, Serializable> user : userList) {
      String sourceUsername = (String) user.get(SOURCE_USERNAME);
      String targetUsername = (String) user.get(TARGET_USERNAME);

      List<SiteInfo> listSites = siteService.listSites(sourceUsername);
      LOG.info("Number of sites is: " + listSites.size());

      for (SiteInfo site : listSites) {
        String membersRole = siteService.getMembersRole(site.getShortName(), sourceUsername);
        if (!test) {
          String targetMemberRole = siteService.getMembersRole(site.getShortName(), targetUsername);
          // Do not replace memberships, just add for new ones
          if (targetMemberRole == null) {
            siteService.setMembership(site.getShortName(), targetUsername, membersRole);
            LOG.info("Adding membership of " + targetUsername + " in site " + site.getShortName() + " with role " + membersRole);
          }
          siteService.removeMembership(site.getShortName(), sourceUsername);
          LOG.info("Removing membership of " + sourceUsername + " in site " + site.getShortName());
        }
      }

      user.put(CHANGE_SITE_MEMBERSHIP_COUNT, listSites.size());
    }
    return userList;
  }
  
  /**
   * Transfer global group memberships from one person to another. 
   * If the source user was a member of a group, then
   * the new user will just be a member of the group as well. 
   * 
   * @param userList
   *          List of users
   * @param test
   *          If set to true no modification of data will be made, if set to
   *          false file ownerships will be made
   * @return
   */
  protected List<Map<String, Serializable>> transferGlobalGroupMemberships(List<Map<String, Serializable>> userList, Boolean test) {

    for (Map<String, Serializable> user : userList) {
      String sourceUsername = (String) user.get(SOURCE_USERNAME);
      String targetUsername = (String) user.get(TARGET_USERNAME);

      Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(sourceUsername);
      Set<String> groups = authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
      
      Set<String> intersection = new HashSet<String>();
      // Since the transfer memberships function will take care of the site memberships, and those groups
      // remove them from the set by getting the intersection of the two sets.
      for (String group : groups){
        if (authoritiesForUser.contains(group)){
          intersection.add(group);
        }
      }
      for (String authority : intersection){
        
        if (!test){
          if (!(authorityService.getAuthoritiesForUser(targetUsername)).contains(authority)){
            authorityService.addAuthority(authority, targetUsername);
          }
          
          LOG.info("Adding " + targetUsername + " to group " + authority);
          authorityService.removeAuthority(authority, sourceUsername);
          LOG.info("Removing " + sourceUsername + " from group " + authority);
        }
      }
      

      user.put(CHANGE_GLOBAL_GROUP_COUNT, authoritiesForUser.size());
    }
    return userList;
  }
 
  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }
  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }
  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }
  public void setOwnableService(OwnableService ownableService) {
    this.ownableService = ownableService;
  }
  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }
  public void setAuthenticationService(MutableAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }
  public void setPermissionService(PermissionService permissionService) {
    this.permissionService = permissionService;
  }
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
  }
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(personService);
    Assert.notNull(nodeService);
    Assert.notNull(siteService);
    Assert.notNull(searchService);
    Assert.notNull(ownableService);
    Assert.notNull(behaviourFilter);
    Assert.notNull(authenticationService);
    Assert.notNull(permissionService);
    Assert.notNull(authorityService, "you have to provide an instance of AuthorityService");
  }


}
