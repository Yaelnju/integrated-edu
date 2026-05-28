<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Students">
    <Students>
      <xsl:for-each select="student">
        <student>
          <id><xsl:value-of select="StuID"/></id>
          <name><xsl:value-of select="StuName"/></name>
          <sex><xsl:value-of select="Gender"/></sex>
          <major><xsl:value-of select="Dept"/></major>
        </student>
      </xsl:for-each>
    </Students>
  </xsl:template>
</xsl:stylesheet>
