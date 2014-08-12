package com.github.ptomli.bedrock.spring;

import static org.fest.assertions.api.Assertions.*;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.Jackson;

import java.io.File;

import javax.validation.Validation;

import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultSpringContextConfigurationTest {

	private ClassLoader cl = DefaultSpringContextConfigurationTest.class.getClassLoader();
	private ConfigurationFactory<MyConfiguration> cf;

	@Before
	public void setup() {
		cf = new ConfigurationFactory<MyConfiguration>(MyConfiguration.class, Validation.buildDefaultValidatorFactory().getValidator(), Jackson.newObjectMapper(), "config");
	}

	@Test
	public void testDefaultValues() throws Exception {
		MyConfiguration c = cf.build(new File(cl.getResource("com/github/ptomli/bedrock/spring/DefaultSpringContextConfigurationTest-empty.yml").toURI()));

		assertThat(c.spring).isNotNull();
		Assertions.<Class<?>>assertThat(c.spring.getApplicationContextClass()).isEqualTo(ClassPathXmlApplicationContext.class);
		assertThat(c.spring.getConfigLocations()).isEqualTo(new String[] { "/META-INF/spring/*.xml" });
		assertThat(c.spring.getProfiles()).isEmpty();
		assertThat(c.spring.getPropertySources()).isEmpty();
	}

	@Test
	public void testAnnotationContext() throws Exception {
		MyConfiguration c = cf.build(new File(cl.getResource("com/github/ptomli/bedrock/spring/DefaultSpringContextConfigurationTest-annotation.yml").toURI()));

		assertThat(c.spring).isNotNull();
		Assertions.<Class<?>>assertThat(c.spring.getApplicationContextClass()).isEqualTo(AnnotationConfigApplicationContext.class);
		assertThat(c.spring.getConfigLocations()).isEqualTo(new String[] { "com.github.ptomli.bedrock" });
		assertThat(c.spring.getProfiles()).isEqualTo(new String[] { "production" });
		assertThat(c.spring.getPropertySources()).isEmpty();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MyConfiguration extends Configuration {
		@JsonProperty
		private DefaultSpringContextConfiguration spring = new DefaultSpringContextConfiguration();
	}
}
