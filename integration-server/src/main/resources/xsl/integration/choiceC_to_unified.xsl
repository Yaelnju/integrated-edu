<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:template match="Choices">
    <Choices>
      <xsl:for-each select="choice">
        <choice>
          <sid><xsl:value-of select="Sno"/></sid>
          <cid><xsl:value-of select="concat('00000', Cno)"/></cid>
          <score><xsl:value-of select="Grd"/></score>
        </choice>
      </xsl:for-each>
    </Choices>
  </xsl:template>
</xsl:stylesheet>
