<?xml version="1.0" encoding="UTF-8"?>

<!--
This stylesheet transforms Coverage XML export for iSYSTEM winIDEA
to XML in Cobertura format, so that it can be used by Cobertura
plugin for Jenkins. Since Cobertura is implemented for Java, the
following mapping was introduced to show C coverage info:
- download file (executable, usually elf file) is mapped to packages
- source file is mapped to class, because in Java one file usually
  contains one class.
- C functions are mapped to methods
- C lines are equvalent to Java source lines
- each condition can be executed as true, false, or both.
  iSYSTEM coverage provides this information, and to show it in Cobertura
  format, number of conditions is multipled by 2. Condition coverage is then
  calculated as:
  (true + false + both *2) / (cn * 2)

  To provide enough information in coverage file, the following information
  must be preent in XML export file:

  - Measure all functions must be checked
  - Function lines must be exported (YES)

  Sources are not needed and are not shown in Jenkins. For this level of detail
  in coverage, export coverage in iSYSTEM HTML format.
-->

<xsl:stylesheet id='isystemTestReportXslt' version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="https://resources.isystem.com/analyzer_coverage_export"
                xpath-default-namespace="https://resources.isystem.com/analyzer_coverage_export">
    
  <xsl:output method="xml" encoding="utf-8"
              doctype-system="http://cobertura.sourceforge.net/xml/coverage-04.dtd"/>

  <xsl:template match="/">
    <coverage timestamp="{/coverage/info/date} - {/coverage/info/time}">
    
      <packages>
          <xsl:apply-templates select="coverage/images/image"/>
      </packages>

    </coverage>
  </xsl:template>


  <xsl:template match="coverage/images/image">
    <package name="{name}">
      <classes>
        <xsl:apply-templates select="folder"/>
      </classes>
    </package>
  </xsl:template>


  <xsl:template match="coverage/images/image/folder">
    <xsl:apply-templates select="module"/>
  </xsl:template>


  <xsl:template match="coverage/images/image/folder/module">
    <class name="{name}" filename="{rel_path}">
      <methods>
        <xsl:apply-templates select="fn"/>
      </methods>
      <lines>
        <xsl:apply-templates select="fn/ln"/>
      </lines>
    </class>
  </xsl:template>


  <xsl:template match="coverage/images/image/folder/module/fn">
    <method name="{name}" hits="{ec}" signature="">
      <lines>
        <xsl:apply-templates select="ln"/>
      </lines>
    </method>
  </xsl:template>


  <xsl:template match="ln">
      <xsl:choose>
          <xsl:when test="cn = 0">
              <line number="{sn}" hits="{ec}" branch="false"/>
          </xsl:when>
          <xsl:otherwise>
              <line number="{sn}" hits="{ec}" branch="true"
                    condition-coverage="{(cn1 + cn0 + cn01 * 2) div (cn*2) *100}% ({cn1 + cn0 + cn01 * 2}/{cn*2})"/>
          </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <!-- condition node is ignored, because Jenkins plugin takes information from
       the string above.
       -->
</xsl:stylesheet>
