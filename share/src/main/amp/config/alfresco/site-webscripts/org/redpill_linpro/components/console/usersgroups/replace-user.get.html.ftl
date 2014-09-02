<@markup id="css" >
  <#-- CSS Dependencies -->
  <@link href="${url.context}/res/components/console/replace-user.css" group="console"/>
</@>

<@markup id="js">
  <#-- JavaScript Dependencies -->
  <@script src="${url.context}/res/components/console/consoletool.js" group="console"/>
  <@script type="text/javascript" src="${url.context}/res/components/console/replace-user.js" group="console" />
</@>

<@markup id="widgets">
  <@createWidgets group="console"/>
</@>

<@markup id="html">
  <@uniqueIdDiv>
  	<#assign el=args.htmlid?html>
  	<div id="${el}-body" class="console-replace-user">
  		<div class="yui-u first">
        <div class="title">${msg("replace-user.title")}</div>
      </div>
      <div id="${el}-form-container">
        <label for="content">${msg("label.content")}</label><br />
        <div class="help">${msg("help.content")}</div>
        <textarea id="${el}-content" name="content"></textarea><br />
        <label for="disableUsers">${msg("label.disableUsers")}</label>
        <input type="checkbox" id="${el}-disableUsers" name="disableUsers" /><br />
        <label for="transferSiteMemberships">${msg("label.transferSiteMemberships")}</label>
        <input type="checkbox" id="${el}-transferSiteMemberships" name="transferSiteMemberships" /><br />
        <label for="transferFileOwnerships">${msg("label.transferFileOwnerships")}</label>
        <input type="checkbox" id="${el}-transferFileOwnerships" name="transferFileOwnerships" /><br />
        <div id="${el}-submit-button-container">
          <button id="${el}-submit-button">${msg("label.submit")}</button>
        </div>

        <div id="${el}-confirm-container">

        </div>
      </div>
  	</div> 
  </@>
</@>
