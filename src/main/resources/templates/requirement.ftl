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

[.requirement, cols=8*, ref="{ref}", summary="{summary}", subsystem="{subsystem}", verification="{verification}", compliance="{compliance}", version="{version}", subsystem="{subsystem}", type="{type}", target="{target}"]
|====

6+s|{ref}/{version}: {summary} ({type})

s|Version
^|{target}

.2+.^s|Satisfies
5.2+a|<#list req.satisfies as parent>
  * ${parent}
</#list>

s|Verification
^|{verification}

s|Compliance
^|{compliance}

8+a|${req.body}

.^s|Note
7+a|${req.note}

|====
</#macro>


<#list requirements as req>
    <@requirement req />

</#list>