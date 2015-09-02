if (typeof RPL == "undefined" || !RPL) {
	var RPL = {};
}

(function()
{
   /**
  * YUI Library aliases
  */
   var Dom = YAHOO.util.Dom,
    Event = YAHOO.util.Event,
    Element = YAHOO.util.Element,
    KeyListener = YAHOO.util.KeyListener;
   
   /**
  * Alfresco Slingshot aliases
  */
   var $html = Alfresco.util.encodeHTML,
    parseURL = Alfresco.util.parseURL
   
   /**
  * ReplaceUser constructor.
  *
  * @param {String} htmlId The HTML id of the parent element
  * @return {RPL.ReplaceUser} The new ReplaceUser instance
  * @constructor
  */
   RPL.ReplaceUser = function(htmlId)
   {
    this.name = "RPL.ReplaceUser";
    RPL.ReplaceUser.superclass.constructor.call(this, htmlId);
    
    /* Register this component */
    Alfresco.util.ComponentManager.register(this);
    
    /* Load YUI Components */
    Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);
     /* Define panel handlers */
    var parent = this;
    
    // NOTE: the panel registered first is considered the "default" view and is displayed first
    
    /* File List Panel Handler */
    ListPanelHandler = function ListPanelHandler_constructor()
    {
     ListPanelHandler.superclass.constructor.call(this, "list");
    };
    
    YAHOO.extend(ListPanelHandler, Alfresco.ConsolePanelHandler, 
    {
     /**
      * Called by the ConsolePanelHandler when this panel shall be loaded
      *
      * @method onLoad
      */
     onLoad: function onLoad()
     {
      
     }
    });
    new ListPanelHandler();
    return this;
   };
   
   YAHOO.extend(RPL.ReplaceUser, Alfresco.ConsoleTool, 
   {
    /**
     * Set multiple initialization options at once.
     *
     * @method setOptions
     * @param obj {object} Object literal specifying a set of options
     */
    setOptions: function ReplaceUser_setOptions(obj)
    {
     this.options = YAHOO.lang.merge(this.options, obj);
     return this;
    },

    lastSearchTerm : "",

    options:
    {
    minSearchTermLength : 0
    },
    
    /**
     * Fired by YUI when parent element is available for scripting.
     * Component initialisation, including instantiation of YUI widgets and event listener binding.
     *
     * @method onReady
     */
    onReady: function ReplaceUser_onReady()
    {
    // Call super-class onReady() method
    RPL.ReplaceUser.superclass.onReady.call(this);     

    // Search button
    this.widgets.submitButton = Alfresco.util.createYUIButton(this, "submit-button", this.onSubmitClick);
    
    this.widgets.content = Dom.get(this.id + "-content");
    this.widgets.disableUsers = Dom.get(this.id + "-disableUsers");
    this.widgets.transferSiteMemberships = Dom.get(this.id + "-transferSiteMemberships");
    this.widgets.transferFileOwnerships = Dom.get(this.id + "-transferFileOwnerships");
    this.widgets.transferGlobalGroups = Dom.get(this.id + "-transferGlobalGroups");

    },

    /**
     * Submit button click event handler
     *
     * @method onSearchClick
     * @param e {object} DomEvent
     * @param args {array} Event parameters (depends on event type)
     */
    onSubmitClick: function ReplaceUser_onSubmitClick(e, args)
    {
      //Populate data from form
      var data = {
        content: this.widgets.content.value,
        disableUsers: this.widgets.disableUsers.checked,
        transferSiteMemberships: this.widgets.transferSiteMemberships.checked,
        transferFileOwnerships: this.widgets.transferFileOwnerships.checked,
        transferGlobalGroups: this.widgets.transferGlobalGroups.checked,
        test: true
      };

      var me = this;

      //Send test request
      var serviceUrl = Alfresco.constants.PROXY_URI + "rpl/admin/replace-user";
      Alfresco.util.Ajax.jsonPost({
        url: serviceUrl,
        successCallback: 
        {
          fn: me.testSuccess,
          scope: me
        },
        failureCallback: 
        {
          fn: me.testFailure,
          scope: me
        },
        dataObj: data
      });
    },

    testSuccess: function ReplaceUser_testSuccess(response) {
      var jsonData = response.json;

      var tableHtml = this.transformResult(jsonData);
      var me = this;
      Alfresco.util.PopupManager.displayPrompt({
        title: this.msg("replace-user.confirm.title", jsonData.records.length),
        text: tableHtml,
        noEscape: true,
        buttons: [
          {
            text: this.msg("button.ok"),
            handler: function() {
              this.destroy();
              //Send test request
              //Populate data from form
              var data = {
                content: me.widgets.content.value,
                disableUsers: me.widgets.disableUsers.checked,
                transferSiteMemberships: me.widgets.transferSiteMemberships.checked,
                transferFileOwnerships: me.widgets.transferFileOwnerships.checked,
                transferGlobalGroups: me.widgets.transferGlobalGroups.checked,
                test: false
              };
              var serviceUrl = Alfresco.constants.PROXY_URI + "rpl/admin/replace-user";
              Alfresco.util.Ajax.jsonPost({
                url: serviceUrl,
                successCallback: 
                {
                  fn: me.success,
                  scope: me
                },
                failureCallback: 
                {
                  fn: me.failure,
                  scope: me
                },
                dataObj: data
              });
            }
          },
          {
            text: this.msg("button.cancel"),
            handler: function() {
              this.destroy();
            },
            isDefault: true
          }]
      });
    },

    testFailure: function ReplaceUser_testFailure(response) {
      if (response.json!=null) {
        var message = response.json.message;
        Alfresco.util.PopupManager.displayPrompt({
          title: this.msg("replace-user.testFailure"),
          text: message
        });
      } else {
        Alfresco.util.PopupManager.displayMessage({
          text: this.msg("replace-user.testFailure")
        });
      }
    },

    success: function ReplaceUser_success(response) {
      var jsonData = response.json;

      var tableHtml = this.transformResult(jsonData);
      var me = this;
      Alfresco.util.PopupManager.displayPrompt({
        title: this.msg("replace-user.result.title", jsonData.records.length),
        text: tableHtml,
        noEscape: true,
        buttons: [
          {
            text: this.msg("button.ok"),
            handler: function() {
              this.destroy();
            },
            isDefault: true
          }]
      });
    },

    failure: function Replace_failure(response) {
      if (response.json!=null) {
        var message = response.json.message;
        Alfresco.util.PopupManager.displayPrompt({
          title: this.msg("replace-user.failure"),
          text: message
        });
      } else {
        Alfresco.util.PopupManager.displayMessage({
          text: this.msg("replace-user.failure")
        });
      }
    },

    transformResult: function ReplaceUser_transformResult(jsonData) {
      var records = jsonData.records;
      var html = "";
      html += "<table>\n";
      html += "<tr>\n";
      html += "<th>"+this.msg("label.table.sourceUsername")+"</th>\n";
      html += "<th>"+this.msg("label.table.sourceFullName")+"</th>\n";
      html += "<th>"+this.msg("label.table.sourceEmail")+"</th>\n";
      html += "<th></th>\n";
      html += "<th>"+this.msg("label.table.targetUsername")+"</th>\n";
      html += "<th>"+this.msg("label.table.targetFullName")+"</th>\n";
      html += "<th>"+this.msg("label.table.targetEmail")+"</th>\n";
      html += "<th></th>\n";
      html += "<th>"+this.msg("label.table.disableUsersCount")+"</th>\n";
      html += "<th>"+this.msg("label.table.changeOwnershipCount")+"</th>\n";
      html += "<th>"+this.msg("label.table.changeSiteMembershipCount")+"</th>\n";
      html += "<th>"+this.msg("label.table.changeGlobalGroupMembershipCount")+"</th>\n";
      html += "</tr>\n";
      for (var i=0; i<records.length; i++) {
        var record = records[i];

        html += "<tr>\n";
        html += "<td>"+record.sourceUsername+"</td>\n";
        html += "<td>"+record.sourceFullName+"</td>\n";
        html += "<td>"+record.sourceEmail+"</td>\n";
        html += "<th>-&gt;</th>\n";
        html += "<td>"+record.targetUsername+"</td>\n";
        html += "<td>"+record.targetFullName+"</td>\n";
        html += "<td>"+record.targetEmail+"</td>\n";
        html += "<th></th>\n";
        html += "<td>"+((record.disableUser==undefined || record.disableUser==null)?this.msg("label.no"):this.msg("label.yes"))+"</td>\n";
        html += "<td>"+((record.changeOwnershipCount==undefined || record.changeOwnershipCount==null)?0:record.changeOwnershipCount)+"</td>\n";
        html += "<td>"+((record.changeSiteMembershipCount==undefined || record.changeSiteMembershipCount==null)?0:record.changeSiteMembershipCount)+"</td>\n";
        html += "<td>"+((record.changeGlobalGroupCount==undefined || record.changeGlobalGroupCount==null)?0:record.changeGlobalGroupCount)+"</td>\n";
        html += "</tr>\n";
      }
      html += "</table>\n";
      return html;
    },
    /**
     * Gets a custom message
     *
     * @method _msg
     * @param messageId {string} The messageId to retrieve
     * @return {string} The custom message
     * @private
     */
    _msg: function ConsoleNodeBrowser__msg(messageId)
    {
     return Alfresco.util.message.call(this, messageId, "RPL.ReplaceUser", Array.prototype.slice.call(arguments).slice(1));
    }
    
   });
})();