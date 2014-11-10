<#compress>
<#escape x as jsonUtils.encodeJSONString(x)>
{
  "records" : [   
    <#list items as item>
    {
      <#list item?keys as key>
        "${key?js_string}": <#if item[key]??>"${item[key]?string}"<#else>null</#if><#if key_has_next>,</#if> 
      </#list>
    }<#if item_has_next>,</#if> 
    </#list>
  ]
}
</#escape>
</#compress>