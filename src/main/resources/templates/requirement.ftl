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

    <#assign total_cols = 8>
    <#if req.satisfies?size == 0 >
        <#assign total_cols = 6>
    </#if>

    <#assign body_cols = total_cols>
    <#assign note_cols = total_cols - 1>

  [.requirement, cols=${total_cols}*, ref="{ref}", summary="{summary}", subsystem="{subsystem}", verification="{verification}", compliance="{compliance}", version="{version}", subsystem="{subsystem}", type="{type}", target="{target}"]
  |====

  6+s|{ref}/{version}: {summary} ({type})

  s|Version
  ^|{target}

    <#if req.satisfies?size gt 0 >
      .2+.^s|Satisfies
      5.2+a|<#list req.satisfies as parent>
      * ${parent}
    </#list>
    </#if>

  s|Verification
  ^|{verification}

  s|Compliance
  ^|{compliance}

    ${body_cols}+a|${req.body}

  .^s|Note
    ${note_cols}+a|${req.note}

  |====
</#macro>


<#list requirements as req>
    <@requirement req />

</#list>