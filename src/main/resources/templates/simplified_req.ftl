<#-- @ftlvariable name="requirements" type="java.util.List<fr.melchiore.tools.requp.data.Requirement>" -->

<#macro requirement req>
<#-- @ftlvariable name="req" type="fr.melchiore.tools.requp.data.Requirement" -->
:ref: ${req.ref}
:summary: ${req.summary}
:subsystem: ${req.subsystems?join(", ")}
:verification: <#list req.verification as verif>${verif.abrv}<#sep>, </#list>
:compliance: ${req.compliance.abrv}
:version: ${req.version}
:type: ${req.type}
:target: ${req.target}

[.requirement, ref="{ref}",summary="{summary}",subsystem="{subsystem}",verification="{verification}",compliance="{compliance}",version="{version}",subsystem="{subsystem}",type="{type}",target="{target}"]
.{ref}/{version}: {summary} ({type})
--

[horizontal]
Version:: {target}
Verification:: {verification}
Compliance:: {compliance}
Subsystems:: {subsystem}
  <#if req.satisfies?size gt 0 >
Satisfies::
      <#list req.satisfies as parent>
 * ${parent}
      </#list>
  </#if>

${req.body}

  <#if req.note?length gt 0 >
NOTE: ${req.note}
  </#if>

--
</#macro>


<#list requirements as req>
    <@requirement req />

</#list>