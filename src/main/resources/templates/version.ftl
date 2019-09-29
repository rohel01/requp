<#-- @ftlvariable name="requirements" type="java.util.List<fr.melchiore.tools.requp.data.Requirement>" -->
<#-- @ftlvariable name="versions" type="java.util.List<com.github.zafarkhaja.semver.Version>" -->

<#macro requirement req versions>
<#-- @ftlvariable name="req" type="fr.melchiore.tools.requp.data.Requirement" -->
<#-- @ftlvariable name="versions" type="java.util.List<com.github.zafarkhaja.semver.Version>" -->
|${req.ref}: ${req.summary}
  <#if req.target?trim?length == 0 >
${versions?size}+^|icon:question[]
  <#else>
<#list versions as version>
^.^|<#if version.satisfies(req.target)>icon:check[]</#if>
</#list>

  </#if>
</#macro>

:icons: font
<#-- We can't use %header, since it messes with cell merging -->
[cols="6, <#list versions as version>1<#sep>, </#list>"]
|====

.2+^.^h|Requirement

${versions?size}+^h|Version

<#list versions as version>
^.^h|${version.majorVersion}.${version.minorVersion}
</#list>

<#list requirements as req>
<@requirement req versions/>

</#list>

|====