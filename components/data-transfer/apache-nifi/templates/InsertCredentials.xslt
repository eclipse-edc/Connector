<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name = "apiUsername" />
	<xsl:param name = "apiPassword" />
	<xsl:param name = "apiStorePasswd" />
	<xsl:param name = "apiKeyPasswd" />
	<xsl:output version="1.0" encoding="UTF-8" standalone="yes"/>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="template/snippet/processors/config/properties/entry/key[../../../../name='SetAuthValues' and .='username']">
		<xsl:copy-of select="."/>
		<value><xsl:value-of select="$apiUsername"/></value>
	</xsl:template>

    <xsl:template match="template/snippet/processors/config/properties/entry/key[../../../../name='SetAuthValues' and .='password']">
		<xsl:copy-of select="."/>
		<value><xsl:value-of select="$apiPassword"/></value>
	</xsl:template>

    <xsl:template match="template/snippet/controllerServices/properties/entry/key[../../../type='org.apache.nifi.ssl.StandardRestrictedSSLContextService' and .='Keystore Password']">
		<xsl:copy-of select="."/>
		<value><xsl:value-of select="$apiStorePasswd"/></value>
	</xsl:template>

    <xsl:template match="template/snippet/controllerServices/properties/entry/key[../../../type='org.apache.nifi.ssl.StandardRestrictedSSLContextService' and .='key-password']">
		<xsl:copy-of select="."/>
		<value><xsl:value-of select="$apiKeyPasswd"/></value>
	</xsl:template>

</xsl:stylesheet>