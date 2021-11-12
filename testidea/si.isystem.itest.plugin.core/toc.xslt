<?xml version="1.0" encoding="UTF-8"?>
<!--
This stylesheet transform converts TOCs for Eclipse help written in
XML to simple HTML file. It can be used to prepare testIDEA help
for publish on company web page.
Customization: change value of 'indent' variable below to change
indentation size.

Usage:

$ java -cp saxon9he.jar  net.sf.saxon.Transform -t -s:toc.xml -xsl:toc.xslt -o:out.html

-->

<xsl:stylesheet id='testIDEAHelpToc'
                version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:it="https://resources.isystem.com/itest_help_toc"
                >

  <xsl:output method="html"
              encoding="utf-8"
              doctype-system="about:legacy-compat"/>

  <xsl:template match="/">
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="#itestHelp.css"/>
      </head>
      <body>
          <xsl:apply-templates select="toc">
              <xsl:with-param name="headingLevel" select="1"/>
          </xsl:apply-templates>

        <!-- to open and process document directly use:
            xsl:apply-templates select="document('help/tocgettingstarted.xml')/toc"/ -->
      </body>
    </html>
  </xsl:template>


  <xsl:template match="toc">
    <xsl:param name="headingLevel"/>

    <!-- level of indentation for subsections in pixels -->
    <xsl:variable name="indent" select='10'/>
    
    <xsl:choose>
        <!-- if href is given, create link, otherwise  'anchor' element must be present. -->
        <xsl:when test='$headingLevel = 1'>
            <h1><xsl:value-of select="@label"/></h1>
        </xsl:when>
        <xsl:when test='$headingLevel = 2'>
            <h2 style="margin-left: {$indent}pt"><xsl:value-of select="@label"/></h2>
        </xsl:when>
        <xsl:when test='$headingLevel = 3'>
            <h3 style="margin-left: {$indent * 2}pt"><xsl:value-of select="@label"/></h3>
        </xsl:when>
        <xsl:otherwise>
            <h4 style="margin-left: {$indent* 3}pt"><xsl:value-of select="@label"/></h4>
        </xsl:otherwise>
    </xsl:choose>

    <div style="margin-left: {($headingLevel - 1) * $indent}pt">

    <xsl:for-each select="topic">

      <xsl:variable name="href" select='@href'/>
      <xsl:variable name="lbl" select='@label'/>
      
      <xsl:choose>
        <!-- if href is given, create link, otherwise  'anchor' element must be present. -->
        <xsl:when test="$href">
            
            <a href="{$href}"><xsl:value-of select="@label"/></a><br/>
            
        </xsl:when>
        <xsl:otherwise>
            <xsl:apply-templates select="anchor">
                <xsl:with-param name="headingLevel" select="$headingLevel"/>
            </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
            
    </xsl:for-each>
    </div>
  </xsl:template>

  <xsl:template match="anchor">
      <xsl:param name="headingLevel"/>
      <xsl:variable name="docName" select='concat("help/toc", @id, ".xml")'/>
      <xsl:apply-templates select="document($docName)/toc">
          <xsl:with-param name="headingLevel" select="$headingLevel + 1"/>
      </xsl:apply-templates>

  </xsl:template>

</xsl:stylesheet>  
