package com.github.ptomli.bedrock;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.github.ptomli.bedrock.spring.SpringServiceConfigurer;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.HttpConfiguration;

public class GitHubIssue1Test {

	@Test
	public void test() {
		Environment environment = mock(Environment.class);
		Configuration configuration = mock(Configuration.class);

		HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
		when(configuration.getHttpConfiguration()).thenReturn(httpConfiguration);

		ConfigurableApplicationContext context = SpringServiceConfigurer.forEnvironment(environment)
			.withContext(ClassPathXmlApplicationContext.class, "classpath:/com/github/ptomli/bedrock/GitHubIssue1Test-context.xml")
			.registerConfigurationBean("dw", configuration)
			.getApplicationContext();

		context.refresh();
		Bean bean = context.getBean("bean", Bean.class);
		assertThat(bean).isNotNull();
		assertThat(bean.httpConfiguration).isSameAs(httpConfiguration);
	}

	public static class Bean {
		@Autowired
		@Value("#{dw.httpConfiguration}")
		public HttpConfiguration httpConfiguration;
	}
}
