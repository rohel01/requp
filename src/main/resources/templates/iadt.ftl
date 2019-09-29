<#-- @ftlvariable name="requirements" type="java.util.List<fr.melchiore.tools.requp.data.Requirement>" -->
<#-- @ftlvariable name="verification" type="fr.melchiore.tools.requp.data.Verification" -->

<#macro requirement req verif>
<#-- @ftlvariable name="req" type="fr.melchiore.tools.requp.data.Requirement" -->
<#-- @ftlvariable name="verif" type="fr.melchiore.tools.requp.data.Verification" -->
|${req.ref}: ${req.summary}
  <#if req.verification?size == 0>
4+^|icon:question[]
  <#else>
^|<#if req.verification?seq_contains(verif.INSPECTION)>icon:check[]</#if>
^|<#if req.verification?seq_contains(verif.ANALYSIS)>icon:check[]</#if>
^|<#if req.verification?seq_contains(verif.DESIGN_REVIEW)>icon:check[]</#if>
^|<#if req.verification?seq_contains(verif.TEST)>icon:check[]</#if>
  </#if>
</#macro>

:icons: font

[%header, cols="6, 1, 1, 1, 1"]
|====

^.^|Requirement
^.^|Inspection
^.^|Analysis
^.^|Design Review
^.^|Test

<#list requirements as req>
    <@requirement req verification/>

</#list>

|====