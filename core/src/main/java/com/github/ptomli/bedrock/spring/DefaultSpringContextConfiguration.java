package com.github.ptomli.bedrock.spring;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertySource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

/**
 * The default {@link SpringContextConfiguration} which can be easily used
 * with a DropWizard {@link Configuration}.
 * <p>
 * Expects YAML like
 * <pre>
 * <code>
 * class: org.springframework.context.support.ClassPathXmlApplicationContext
 * locations:
 *   - /META-INF/spring/*.xml
 * profiles:
 *   - production
 * </code>
 * </pre>
 */
public class DefaultSpringContextConfiguration implements SpringContextConfiguration {

	@JsonProperty("class")
	private Class<? extends ConfigurableApplicationContext> clazz = ClassPathXmlApplicationContext.class;

	@JsonProperty
	private String[] locations = new String[] { "/META-INF/spring/*.xml" };

	@JsonProperty
	private String[] profiles = new String[] {};

	@Override
	public Class<? extends ConfigurableApplicationContext> getApplicationContextClass() {
		return this.clazz;
	}

	@Override
	public String[] getConfigLocations() {
		return this.locations;
	}

	@Override
	public String[] getProfiles() {
		return this.profiles;
	}

	@Override
	public PropertySource<?>[] getPropertySources() {
		return new PropertySource<?>[] {};
	}

}
