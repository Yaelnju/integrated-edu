<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <!-- 将院系 C 本地课程 XML 转为集成端统一课程格式（课件表 3-17 映射） -->
  <xsl:template match="Courses">
    <Classes>
      <xsl:for-each select="course">
        <class>
          <id>
            <xsl:value-of select="concat('00000', Cno)"/>
          </id>
          <name><xsl:value-of select="Cnm"/></name>
          <time><xsl:value-of select="Ctm"/></time>
          <score><xsl:value-of select="Cpt"/></score>
          <teacher><xsl:value-of select="Tec"/></teacher>
          <location><xsl:value-of select="Pla"/></location>
          <share><xsl:value-of select="Share"/></share>
        </class>
      </xsl:for-each>
    </Classes>
  </xsl:template>
</xsl:stylesheet>
