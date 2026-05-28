<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Courses">
    <Classes>
      <xsl:for-each select="course">
        <class>
          <id><xsl:value-of select="CourseID"/></id>
          <name><xsl:value-of select="CourseName"/></name>
          <time><xsl:value-of select="Credit * 16"/></time>
          <score><xsl:value-of select="Credit"/></score>
          <teacher><xsl:value-of select="Teacher"/></teacher>
          <location>A-校区</location>
          <share><xsl:value-of select="IsShared"/></share>
        </class>
      </xsl:for-each>
    </Classes>
  </xsl:template>
</xsl:stylesheet>
