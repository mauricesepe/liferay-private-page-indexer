package com.liferay.search.crawler.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

@Meta.OCD(
	id = "com.liferay.search.crawler.configuration.CrawlerConfiguration",
	name = "crawler-configuration",
	localization = "content/Language"
)
@ExtendedObjectClassDefinition(
		category = "crawler", scope = ExtendedObjectClassDefinition.Scope.SYSTEM
)
public interface CrawlerConfiguration {

	@Meta.AD(
		deflt = "false",
		name = "auto-authenticate",
		required = false
	)
	public boolean autoAthenticate();

	@Meta.AD(
		deflt = "",
		name = "crawler-id",
		required = false
	)
	public String crawlerId();

	@Meta.AD(
		deflt = "",
		name = "crawler-password", required = false
	)
	public String crawlerPassword();

	@Meta.AD(
		deflt = "400",
		name = "search-summary-max-length", required = false
	)
	public int searchSummaryMaxLength();
}