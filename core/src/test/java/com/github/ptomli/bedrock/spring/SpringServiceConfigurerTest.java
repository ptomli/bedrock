package com.github.ptomli.bedrock.spring;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import io.dropwizard.Configuration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Environment;

import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sun.jersey.spi.inject.InjectableProvider;


public class SpringServiceConfigurerTest {
	private static final String EMPTY_CONTEXT = "com/github/ptomli/bedrock/spring/empty-context.xml";

	private SpringServiceConfigurer configurer;
	private ConfigurableApplicationContext springContext;
	private ConfigurableListableBeanFactory springBeanFactory;
	private ConfigurableEnvironment springEnvironment;

	private Environment dwEnvironment;
	private Configuration dwConfiguration;

	private ServletEnvironment servlets;
	private JerseyEnvironment jersey;
	private AdminEnvironment admin;
	private LifecycleEnvironment lifecycle;
	private HealthCheckRegistry healthchecks;

	@Before
	public void setup() {
		springContext = mock(ConfigurableApplicationContext.class);

		springBeanFactory = mock(ConfigurableListableBeanFactory.class);
		when(springContext.getBeanFactory()).thenReturn(springBeanFactory);

		springEnvironment = mock(ConfigurableEnvironment.class);
		when(springContext.getEnvironment()).thenReturn(springEnvironment);

		servlets = mock(ServletEnvironment.class);
		jersey = mock(JerseyEnvironment.class);
		admin = mock(AdminEnvironment.class);
		lifecycle = mock(LifecycleEnvironment.class);
		healthchecks = mock(HealthCheckRegistry.class);

		dwConfiguration = mock(Configuration.class);

		dwEnvironment = mock(Environment.class);
		when(dwEnvironment.servlets()).thenReturn(servlets);
		when(dwEnvironment.jersey()).thenReturn(jersey);
		when(dwEnvironment.admin()).thenReturn(admin);
		when(dwEnvironment.lifecycle()).thenReturn(lifecycle);
		when(dwEnvironment.healthChecks()).thenReturn(healthchecks);

		configurer = SpringServiceConfigurer.forEnvironment(dwEnvironment);
	}

	@Test
	public void testClassPathXmlApplicationContext() {
		springContext = configurer.withContext(ClassPathXmlApplicationContext.class)
		                          .getApplicationContext();

		assertThat(springContext).isInstanceOf(ClassPathXmlApplicationContext.class);
	}

	@Test
	public void testAnnotationConfigApplicationContext() {
		springContext = configurer.withContext(AnnotationConfigApplicationContext.class)
		                          .getApplicationContext();

		assertThat(springContext).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test
	public void testContextConfigurationWithClassPathXmlApplicationContext() {
		SpringContextConfiguration config = mock(SpringContextConfiguration.class);
		Mockito.<Class<?>>when(config.getApplicationContextClass()).thenReturn(ClassPathXmlApplicationContext.class);
		when(config.getConfigLocations()).thenReturn(new String[] {});
		when(config.getProfiles()).thenReturn(new String[] {});
		when(config.getPropertySources()).thenReturn(new PropertySource<?>[] {});

		springContext = configurer.withContextConfiguration(config)
		                          .getApplicationContext();

		assertThat(springContext).isInstanceOf(ClassPathXmlApplicationContext.class);
	}

	@Test
	public void testContextConfigurationWithAnnotationConfigApplicationContext() {
		SpringContextConfiguration config = mock(SpringContextConfiguration.class);
		Mockito.<Class<?>>when(config.getApplicationContextClass()).thenReturn(AnnotationConfigApplicationContext.class);
		when(config.getConfigLocations()).thenReturn(new String[] {});
		when(config.getProfiles()).thenReturn(new String[] {});
		when(config.getPropertySources()).thenReturn(new PropertySource<?>[] {});

		springContext = configurer.withContextConfiguration(config)
		                          .getApplicationContext();

		assertThat(springContext).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test(expected = ApplicationContextInstantiationException.class)
	public void testContextConfigurationWithUnknowngApplicationContext() {
		SpringContextConfiguration config = mock(SpringContextConfiguration.class);
		Mockito.<Class<?>>when(config.getApplicationContextClass()).thenReturn(ConfigurableApplicationContext.class);

		configurer.withContextConfiguration(config);
	}

	@Test(expected = IllegalStateException.class)
	public void testResetRootContextThrowsException() {
		configurer.withContext(springContext).withContext(springContext);
	}

	@Test(expected = IllegalStateException.class)
	public void testRegisterPropertySourceAfterRefreshThrowsException() {
		when(springContext.isActive()).thenReturn(true);
		configurer.withContext(springContext).registerConfigurationPropertySource(null, null);
	}

	@Test
	public void testRegisterEnvironment() {
		configurer.withContext(ClassPathXmlApplicationContext.class, EMPTY_CONTEXT).registerEnvironment("env");
		ConfigurableApplicationContext context = configurer.getApplicationContext();
		if (!context.isActive()) {
			context.refresh();
		}
		assertThat(context.getBean("env")).isSameAs(dwEnvironment);
	}

	@Test
	public void testRegisterConfigurationPropertySourceRegistersEnvironmentPropertySource() {
		MutablePropertySources sources = mock(MutablePropertySources.class);
		when(springEnvironment.getPropertySources()).thenReturn(sources);
		configurer.withContext(springContext).registerConfigurationPropertySource("dw", dwConfiguration);
		verify(sources).addFirst(Matchers.<PropertySource<?>>any());
	}

	// we can't register a configuration bean into the parent context if it was
	// created outside of the configurer
	// TODO: this can possibly be relaxed with the limitation that a configuration bean
	//       registered into the same context is not available during refresh
	@Test(expected = IllegalStateException.class)
	public void registerConfigurationBeanWithExistingParentThrowsException() {
		when(springContext.getParent()).thenReturn(mock(ConfigurableApplicationContext.class));
		configurer.withContext(springContext).registerConfigurationBean("dw", dwConfiguration);
	}

	@Test
	public void testRegisterHealthChecksRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerHealthChecks();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterHealthChecksRegisters() {
		HealthCheck o = mock(HealthCheck.class);
		when(springContext.getBeansOfType(HealthCheck.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerHealthChecks();
		verify(healthchecks).register(anyString(), eq(o));
	}

	@Test
	public void testRegisterProvidersRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerProviders();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterProvidersRegisters() {
		Object o = new Object();
		when(springContext.getBeansWithAnnotation(Provider.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerProviders();
		verify(jersey).register(o);
	}

	@Test
	public void testRegisterInjectableProvidersRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerInjectableProviders();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterInjectableProvidersRegisters() {
		@SuppressWarnings("rawtypes")
		InjectableProvider o = mock(InjectableProvider.class);
		when(springContext.getBeansOfType(InjectableProvider.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerInjectableProviders();
		verify(jersey).register(o);
	}

	@Test
	public void testRegisterResourcesRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerResources();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterResourcesRegisters() {
		Object o = new Object();
		when(springContext.getBeansWithAnnotation(Path.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerResources();
		verify(jersey).register(o);
	}

	@Test
	public void testRegisterTasksRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerTasks();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterTasksRegisters() {
		Task o = mock(Task.class);
		when(springContext.getBeansOfType(Task.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerTasks();
		 verify(admin).addTask(o);
	}

	@Test
	public void testRegisterManagedRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerManaged();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterManagedRegisters() {
		Managed o = mock(Managed.class);
		when(springContext.getBeansOfType(Managed.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerManaged();
		verify(lifecycle).manage(o);
	}

	@Test
	public void testRegisterLifeCyclesRefreshesContext() {
		when(springContext.isActive()).thenReturn(false);
		configurer.withContext(springContext).registerLifeCycles();
		verify(springContext).refresh();
	}

	@Test
	public void testRegisterLifeCyclesRegisters() {
		LifeCycle o = mock(LifeCycle.class);
		when(springContext.getBeansOfType(LifeCycle.class)).thenReturn(Collections.singletonMap("o", o));
		configurer.withContext(springContext).registerLifeCycles();
		verify(lifecycle).manage(o);
	}

	@Test
	public void testRegisterSecurity() {
		// it's required that there's a bean called 'springSecurityFilterChain' in the context, of type Filter
		when(springContext.getBean("springSecurityFilterChain", Filter.class)).thenReturn(mock(Filter.class));

		FilterRegistration.Dynamic registration = mock(FilterRegistration.Dynamic.class);
		when(servlets.addFilter(anyString(), any(Filter.class))).thenReturn(registration);

		configurer.withContext(springContext).registerSpringSecurityFilter("/*");

		verify(servlets).addFilter(anyString(), any(Filter.class));
		verify(registration).addMappingForUrlPatterns(eq(EnumSet.of(DispatcherType.REQUEST)), eq(true), eq("/*"));
	}

	@Test
	public void testParentContextIsRefreshed() throws Exception {
		SpringServiceConfigurer.forEnvironment(dwEnvironment)
			.withContext(ClassPathXmlApplicationContext.class, EMPTY_CONTEXT)
			.registerResources();
	}

	@org.springframework.context.annotation.Configuration
	private static class Config {}
}
