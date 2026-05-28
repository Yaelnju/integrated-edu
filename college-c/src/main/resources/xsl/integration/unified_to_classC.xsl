<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <!-- 统一课程格式 → 院系 C 本地 Courses/course 结构 -->
  <xsl:template match="Classes">
    <Courses>
      <xsl:for-each select="class">
        <course>
          <Cno>
            <xsl:variable name="id9" select="id"/>
            <xsl:value-of select="substring($id9, string-length($id9) - 3)"/>
          </Cno>
          <Cnm><xsl:value-of select="name"/></Cnm>
          <Ctm><xsl:value-of select="time"/></Ctm>
          <Cpt><xsl:value-of select="score"/></Cpt>
          <Tec><xsl:value-of select="teacher"/></Tec>
          <Pla><xsl:value-of select="location"/></Pla>
          <Share><xsl:value-of select="share"/></Share>
        </course>
      </xsl:for-each>
    </Courses>
  </xsl:template>
</xsl:stylesheet>
