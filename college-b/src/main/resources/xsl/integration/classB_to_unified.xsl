<?xml version="1.0" encoding="UTF-8"?>
<!--
  院系 B 本地课程 XML → 集成端统一课程格式
  字段映射：CRS_NO→id, CRS_NAME→name, PERIODS→time, CREDIT→score,
            TEACHER→teacher, LOCATION→location, SHARED→share
  注意：canonical id 约定 9 位定长；B 的 CRS_NO 是 5 位 → 前补 4 个 '0'
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Courses">
    <Classes>
      <xsl:for-each select="course">
        <class>
          <id>
            <xsl:value-of select="concat('0000', CRS_NO)"/>
          </id>
          <name><xsl:value-of select="CRS_NAME"/></name>
          <time><xsl:value-of select="PERIODS"/></time>
          <score><xsl:value-of select="CREDIT"/></score>
          <teacher><xsl:value-of select="TEACHER"/></teacher>
          <location><xsl:value-of select="LOCATION"/></location>
          <share><xsl:value-of select="SHARED"/></share>
        </class>
      </xsl:for-each>
    </Classes>
  </xsl:template>
</xsl:stylesheet>
