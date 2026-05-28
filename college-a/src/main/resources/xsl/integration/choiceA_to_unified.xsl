<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Choices">
    <Choices>
      <xsl:for-each select="choice">
        <choice>
          <sid><xsl:value-of select="StuID"/></sid>
          <cid><xsl:value-of select="CourseID"/></cid>
          <score><xsl:value-of select="Grade"/></score>
        </choice>
      </xsl:for-each>
    </Choices>
  </xsl:template>
</xsl:stylesheet>
