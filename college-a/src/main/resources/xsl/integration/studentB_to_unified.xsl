<?xml version="1.0" encoding="UTF-8"?>
<!--
  院系 B 本地学生 XML → 集成端统一学生格式
  字段映射：STU_NO→id, STU_NAME→name, SEX→sex, MAJOR→major
  PWD 不导出（跨院不带密码）
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Students">
    <Students>
      <xsl:for-each select="student">
        <student>
          <id><xsl:value-of select="STU_NO"/></id>
          <name><xsl:value-of select="STU_NAME"/></name>
          <sex><xsl:value-of select="SEX"/></sex>
          <major><xsl:value-of select="MAJOR"/></major>
        </student>
      </xsl:for-each>
    </Students>
  </xsl:template>
</xsl:stylesheet>
