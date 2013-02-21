package com.github.ptomli.bedrock.spring;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.validation.Validator;

public class DefaultSpringContextConfigurationTest {

	ClassLoader cl = DefaultSpringContextConfigurationTest.class.getClassLoader();

	@Test
	public void testDefaultValues() throws Exception {
		ConfigurationFactory<MyConfiguration> f = ConfigurationFactory.forClass(MyConfiguration.class, new Validator());
		MyConfiguration c = f.build(new File(cl.getResource("com/github/ptomli/bedrock/spring/DefaultSpringContextConfigurationTest-empty.yml").toURI()));

		assertThat(c.spring).isNotNull();
		assertThat(c.spring.getApplicationContextClass()).isEqualTo(ClassPathXmlApplicationContext.class);
		assertThat(c.spring.getConfigLocations()).isEqualTo(new String[] { "/META-INF/spring/*.xml" });
		assertThat(c.spring.getProfiles()).isEmpty();
		assertThat(c.spring.getPropertySources()).isEmpty();
	}

	@Test
	public void testAnnotationContext() throws Exception {
		ConfigurationFactory<MyConfiguration> f = ConfigurationFactory.forClass(MyConfiguration.class, new Validator());
		MyConfiguration c = f.build(new File(cl.getResource("com/github/ptomli/bedrock/spring/DefaultSpringContextConfigurationTest-annotation.yml").toURI()));

		assertThat(c.spring).isNotNull();
		assertThat(c.spring.getApplicationContextClass()).isEqualTo(AnnotationConfigApplicationContext.class);
		assertThat(c.spring.getConfigLocations()).isEqualTo(new String[] { "com.github.ptomli.bedrock" });
		assertThat(c.spring.getProfiles()).isEqualTo(new String[] { "production" });
		assertThat(c.spring.getPropertySources()).isEmpty();
	}

	public static class MyConfiguration extends Configuration {
		@JsonProperty
		private DefaultSpringContextConfiguration spring = new DefaultSpringContextConfiguration();
	}
}
