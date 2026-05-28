<?xml version="1.0" encoding="UTF-8"?>
<!--
  统一课程格式 → 院系 B 本地 Courses/course 结构
  canonical id 9 位，B 的 CRS_NO 5 位 → 取最后 5 位
  substring(s, n) 在 XPath 1.0 是 1-indexed，从第 n 个字符到末尾
  string-length($id9)-4 = 9-4 = 5，于是从第 5 位开始取，得到 5 位字符
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Classes">
    <Courses>
      <xsl:for-each select="class">
        <course>
          <CRS_NO>
            <xsl:variable name="id9" select="id"/>
            <xsl:value-of select="substring($id9, string-length($id9) - 4)"/>
          </CRS_NO>
          <CRS_NAME><xsl:value-of select="name"/></CRS_NAME>
          <PERIODS><xsl:value-of select="time"/></PERIODS>
          <CREDIT><xsl:value-of select="score"/></CREDIT>
          <TEACHER><xsl:value-of select="teacher"/></TEACHER>
          <LOCATION><xsl:value-of select="location"/></LOCATION>
          <SHARED><xsl:value-of select="share"/></SHARED>
        </course>
      </xsl:for-each>
    </Courses>
  </xsl:template>
</xsl:stylesheet>
