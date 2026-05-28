<?xml version="1.0" encoding="UTF-8"?>
<!--
  统一学生格式 → 院系 B 本地 Students/student 结构
  PWD 默认 '000000'，外院学生导入 B 后由学生自行修改
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Students">
    <Students>
      <xsl:for-each select="student">
        <student>
          <STU_NO><xsl:value-of select="id"/></STU_NO>
          <STU_NAME><xsl:value-of select="name"/></STU_NAME>
          <SEX><xsl:value-of select="sex"/></SEX>
          <MAJOR><xsl:value-of select="major"/></MAJOR>
          <PWD>000000</PWD>
        </student>
      </xsl:for-each>
    </Students>
  </xsl:template>
</xsl:stylesheet>
