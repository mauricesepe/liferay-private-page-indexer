package com.liferay.search.crawler.configuration;

import com.liferay.portal.kernel.settings.definition.ConfigurationBeanDeclaration;

import org.osgi.service.component.annotations.Component;

@Component
public class CrawlerConfigurationBeanDeclaration
	implements ConfigurationBeanDeclaration {

	public Class<?> getConfigurationBeanClass() {
		return CrawlerConfiguration.class;
	}

}