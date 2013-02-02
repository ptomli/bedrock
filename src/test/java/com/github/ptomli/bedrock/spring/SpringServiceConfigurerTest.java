package com.github.ptomli.bedrock.spring;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.tasks.Task;
import com.yammer.metrics.core.HealthCheck;


public class SpringServiceConfigurerTest {

	private SpringServiceConfigurer<ConfigurableApplicationContext> configurer;
	private ConfigurableApplicationContext springContext;
	private ConfigurableListableBeanFactory springBeanFactory;
	private ConfigurableEnvironment springEnvironment;

	private Environment dwEnvironment;
	private Configuration dwConfiguration;

	public void foo() {
		SpringServiceConfigurer.forContext(ClassPathXmlApplicationContext.class, "");
		SpringServiceConfigurer.forContext(AnnotationConfigApplicationContext.class, Object.class);

		SpringServiceConfigurer.forContext(ClassPathXmlApplicationContext.class, "")
			.registerConfigurationBean(null, null)
			.registerConfigurationPropertySource("dw", null)
			.registerHealthChecks(null);
	}

	@Before
	public void setup() {
		springContext = mock(ConfigurableApplicationContext.class);

		springBeanFactory = mock(ConfigurableListableBeanFactory.class);
		when(springContext.getBeanFactory()).thenReturn(springBeanFactory);

		springEnvironment = mock(ConfigurableEnvironment.class);
		when(springContext.getEnvironment()).thenReturn(springEnvironment);

		dwConfiguration = mock(Configuration.class);
		configurer = SpringServiceConfigurer.forContext(springContext);

		dwEnvironment = mock(Environment.class);
	}

	@Test
	public void testClassPathXmlApplicationContext() {
		springContext = SpringServiceConfigurer.forContext(ClassPathXmlApplicationContext.class).getApplicationContext();
		assertThat(springContext).isInstanceOf(ClassPathXmlApplicationContext.class);
	}

	@Test
	public void testAnnotationConfigApplicationContext() {
		springContext = SpringServiceConfigurer.forContext(AnnotationConfigApplicationContext.class, Config.class).getApplicationContext();
		assertThat(springContext).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test(expected = IllegalStateException.class)
	public void testRegisterPropertySourceAfterRefreshThrowsException() {
		when(springContext.isActive()).thenReturn(true);
		SpringServiceConfigurer.forContext(springContext).registerConfigurationPropertySource(null, null);
	}

	@Test
	public void testRegisterConfigurationPropertySourceRegistersEnvironmentPropertySource() {
		MutablePropertySources sources = mock(MutablePropertySources.class);
		when(springEnvironment.getPropertySources()).thenReturn(sources);
		configurer.registerConfigurationPropertySource("dw", dwConfiguration);
		verify(sources).addFirst(Matchers.<PropertySource<?>>any());
	}

	@Test
	public void testRegisterConfigurationBeanRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerConfigurationBean("dw", dwConfiguration);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterConfigurationBeanRegistersSingleton() {
		configurer.registerConfigurationBean("dw", dwConfiguration);
		verify(springBeanFactory).registerSingleton("dw", dwConfiguration);
	}

	@Test
	public void testRegisterHealthChecksRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerHealthChecks(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterHealthChecksRegisters() {
		HealthCheck o = mock(HealthCheck.class);
		when(springContext.getBeansOfType(HealthCheck.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerHealthChecks(dwEnvironment);
		verify(dwEnvironment).addHealthCheck(o);
	}

	@Test
	public void testRegisterProvidersRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerProviders(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterProvidersRegisters() {
		Object o = new Object();
		when(springContext.getBeansWithAnnotation(Provider.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerProviders(dwEnvironment);
		verify(dwEnvironment).addProvider(o);
	}

	@Test
	public void testRegisterInjectableProvidersRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerInjectableProviders(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterInjectableProvidersRegisters() {
		@SuppressWarnings("rawtypes")
		InjectableProvider o = mock(InjectableProvider.class);
		when(springContext.getBeansOfType(InjectableProvider.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerInjectableProviders(dwEnvironment);
		verify(dwEnvironment).addProvider(o);
	}

	@Test
	public void testRegisterResourcesRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerResources(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterResourcesRegisters() {
		Object o = new Object();
		when(springContext.getBeansWithAnnotation(Path.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerResources(dwEnvironment);
		verify(dwEnvironment).addResource(o);
	}

	@Test
	public void testRegisterTasksRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerTasks(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterTasksRegisters() {
		Task o = mock(Task.class);
		when(springContext.getBeansOfType(Task.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerTasks(dwEnvironment);
		verify(dwEnvironment).addTask(o);
	}

	@Test
	public void testRegisterManagedRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerManaged(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterManagedRegisters() {
		Managed o = mock(Managed.class);
		when(springContext.getBeansOfType(Managed.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerManaged(dwEnvironment);
		verify(dwEnvironment).manage(o);
	}

	@Test
	public void testRegisterLifeCyclesRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.registerLifeCycles(dwEnvironment);
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterLifeCyclesRegisters() {
		LifeCycle o = mock(LifeCycle.class);
		when(springContext.getBeansOfType(LifeCycle.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.registerLifeCycles(dwEnvironment);
		verify(dwEnvironment).manage(o);
	}

	@org.springframework.context.annotation.Configuration
	private static class Config {}
}
