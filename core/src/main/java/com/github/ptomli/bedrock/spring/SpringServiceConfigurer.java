package com.github.ptomli.bedrock.spring;

import java.util.Map;

import javax.servlet.Filter;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.tasks.Task;
import com.yammer.metrics.core.HealthCheck;

/**
 * Utility class to assist in creation of a Spring ApplicationContext, and the
 * registering of beans defined in that context into the DropWizard
 * environment.
 * <p>
 * Allows you to either supply an existing ApplicationContext, or specify which
 * class should be used, and the configuration.
 * <p>
 * For example, in your DropWizard Service class
 * <pre>
 * <code>
 * {@literal @}Override
 * public void run(Configuration configuration, Environment environment) {
 *     SpringServiceConfigurer.forEnvironment(environment)
 *         .withContext(ClassPathXmlApplicationContext.class, "classpath:/META-INF/spring/applicationContext.xml")
 *         .registerConfigurationPropertySource("dw.", configuration)
 *         .registerConfigurationBean("dw", configuration)
 *         .registerSpringSecurityFilter("/*")
 *         .registerHealthChecks()
 *         .registerResources();
 * }
 * </code>
 * </pre>
 */
public class SpringServiceConfigurer {
	private static final Logger LOG = LoggerFactory.getLogger(SpringServiceConfigurer.class);

	/**
	 * Create a new configurer instance for the given environment.
	 * 
	 * @param environment
	 * @return the configurer
	 */
	public static SpringServiceConfigurer forEnvironment(final Environment environment) {
		return new SpringServiceConfigurer(environment);
	}

	protected static <C extends ConfigurableApplicationContext> C buildContext(final Class<C> clazz, final ApplicationContext parent) {
		try {
			final C context = clazz.newInstance();
			context.setParent(parent);
			return context;
		}
		catch (IllegalAccessException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
		catch (InstantiationException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
	}

	private final Environment environment;
	private final ConfigurableApplicationContext parent = new StaticApplicationContext();
	private ConfigurableApplicationContext context;

	protected SpringServiceConfigurer(final Environment environment) {
		this.environment = environment;
	}

	/**
	 * Set the application context.
	 * 
	 * @param context
	 * @return this configurer
	 * @throws IllegalStateException if the application context has already been set
	 */
	public SpringServiceConfigurer withContext(final ConfigurableApplicationContext context) {
		if (this.context != null) {
			throw new IllegalStateException("context has already been set");
		}
		this.context = context;
		return this;
	}

	/**
	 * Create a new application context of the provided class, using the provided
	 * configuration locations , and set this as the application context.
	 * 
	 * @param clazz the application context class
	 * @param configurations the context configuration locations
	 * @return this configurer
	 * @throws ApplicationContextInstantiationException if there was a problem creating the application context
	 * @throws IllegalStateException if the application context has already been set
	 */
	public SpringServiceConfigurer withContext(final Class<? extends AbstractRefreshableConfigApplicationContext> clazz, final String... configurations) {
		final AbstractRefreshableConfigApplicationContext context = buildContext(clazz, parent);
		context.setConfigLocations(configurations);
		return this.withContext(context);
	}

	/**
	 * Create a new application context of the provided class, using the provided
	 * configuration classes, and set this as the application context.
	 * 
	 * @param clazz the application context class
	 * @param configurations the configuration classes
	 * @return this configurer
	 * @throws ApplicationContextInstantiationException if there was a problem creating the application context
	 * @throws IllegalStateException if the application context has already been set
	 */
	public SpringServiceConfigurer withContext(final Class<? extends AnnotationConfigApplicationContext> clazz, final Class<?>... configurations) {
		final AnnotationConfigApplicationContext context = buildContext(clazz, parent);
		if (configurations.length > 0) {
			context.register(configurations);
		}
		return this.withContext(context);
	}

	/**
	 * Return the application context.
	 * 
	 * @return the application context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return context;
	}

	/**
	 * Register a PropertySource into the Spring Environment for use with a
	 * PropertySourcesPlaceholderConfigurer.
	 * <p>
	 * Property names within the application context are prefixed with
	 * the provided string. If the configuration contains a value accessible
	 * as "foo", and the provided prefix is "dw.", then the property name to
	 * use within the Spring configuration is "dw.foo".
	 * 
	 * @param prefix the string prefix to use
	 * @param configuration the configuration to use for property values
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 * @throws IllegalStateException if the context has already been refreshed
	 */
	public SpringServiceConfigurer registerConfigurationPropertySource(final String prefix, final Configuration configuration) {
		ConfigurableApplicationContext context = this.getRequiredContext();
		if (context.isActive()) {
			throw new IllegalStateException("cannot register a property source after the context has been refreshed");
		}

		final PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(configuration);
		final PropertySource<?> propertySource = new PropertyAccessorPropertySource("dropwizard-config", prefix, accessor);
		context.getEnvironment()
		       .getPropertySources()
		       .addFirst(propertySource);

		return this;
	}

	/**
	 * Register the configuration instance as a Spring bean, using the provided
	 * name as the bean name.
	 * <p>
	 * The bean is registered into the parent application context, to allow
	 * references to the bean to be available during
	 * {@link ConfigurableApplicationContext#refresh() refresh}.
	 * 
	 * @param name the name of the bean in the Spring context
	 * @param configuration the bean to register
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 * @throws IllegalStateException if the application context parent was not created by this configurer
	 */
	public SpringServiceConfigurer registerConfigurationBean(final String name, final Configuration configuration) {
		ConfigurableApplicationContext context = this.getRequiredContext();
		if (context.getParent() != this.parent) {
			throw new IllegalStateException("Cannot register configuration bean into the parent context, this configurer did not create it");
		}
		if (!this.parent.isActive()) {
			this.parent.refresh();
		}
		this.parent.getBeanFactory().registerSingleton(name, configuration);
		return this;
	}

	/**
	 * Register the Spring Security filter chain with the environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @param urlPattern
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerSpringSecurityFilter(final String urlPattern) {
		return this.registerSpringSecurityFilter(urlPattern, "springSecurityFilterChain");
	}

	protected SpringServiceConfigurer registerSpringSecurityFilter(final String urlPattern, final String name) {
		this.environment.addFilter(new DelegatingFilterProxy(this.getRequiredRefreshedContext().getBean(name, Filter.class)), urlPattern);
		return this;
	}

	/**
	 * Registers HealthCheck beans defined in the application context with
	 * the environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerHealthChecks() {
		final Map<String,HealthCheck> beans = this.getRequiredRefreshedContext().getBeansOfType(HealthCheck.class);
		for (final Map.Entry<String,HealthCheck> entry : beans.entrySet()) {
			LOG.info("registering HealthCheck: {}", entry.getValue());
			this.environment.addHealthCheck(entry.getValue());
		}
		return this;
	}

	/**
	 * Register Provider beans defined in the application context with the
	 * environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerProviders() {
		final Map<String, Object> beans = this.getRequiredRefreshedContext().getBeansWithAnnotation(Provider.class);
		for (final Map.Entry<String,Object> entry : beans.entrySet()) {
			LOG.info("registering @Provider: {}", entry.getValue());
			this.environment.addProvider(entry.getValue());
		}
		return this;
	}

	/**
	 * Register InjectableProvider beans defined in the application context with
	 * the environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	@SuppressWarnings("rawtypes")
	public SpringServiceConfigurer registerInjectableProviders() {
		final Map<String,InjectableProvider> beans = this.getRequiredRefreshedContext().getBeansOfType(InjectableProvider.class);
		for (final Map.Entry<String,InjectableProvider> entry : beans.entrySet()) {
			LOG.info("registering InjectableProvider: {}", entry.getValue());
			this.environment.addProvider(entry.getValue());
		}
		return this;
	}

	/**
	 * Register resource beans, annotated with Path, defined in the application
	 * context with the environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerResources() {
		final Map<String,Object> beans = this.getRequiredRefreshedContext().getBeansWithAnnotation(Path.class);
		for (final Map.Entry<String,Object> entry : beans.entrySet()) {
			LOG.info("registering @Path resource: {}", entry.getValue());
			this.environment.addResource(entry.getValue());
		}
		return this;
	}

	/**
	 * Register Task beans defined in the application context with the
	 * environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerTasks() {
		final Map<String,Task> beans = this.getRequiredRefreshedContext().getBeansOfType(Task.class);
		for (final Map.Entry<String,Task> entry : beans.entrySet()) {
			LOG.info("registering Task: {}", entry.getValue());
			this.environment.addTask(entry.getValue());
		}
		return this;
	}

	/**
	 * Register Managed beans defined in the application context with the
	 * environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerManaged() {
		final Map<String,Managed> beans = this.getRequiredRefreshedContext().getBeansOfType(Managed.class);
		for (final Map.Entry<String,Managed> entry : beans.entrySet()) {
			LOG.info("registering Managed: {}", entry.getValue());
			this.environment.manage(entry.getValue());
		}
		return this;
	}

	/**
	 * Register LifeCycle beans defined in the application context with the
	 * environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @return this configurer
	 * @throws IllegalStateException if no application context has been set
	 */
	public SpringServiceConfigurer registerLifeCycles() {
		final Map<String,LifeCycle> beans = this.getRequiredRefreshedContext().getBeansOfType(LifeCycle.class);
		for (final Map.Entry<String,LifeCycle> entry : beans.entrySet()) {
			LOG.info("registering LifeCycle: {}", entry.getValue());
			this.environment.manage(entry.getValue());
		}
		return this;
	}

	protected ConfigurableApplicationContext getRequiredContext() {
		if (this.context == null) {
			throw new IllegalStateException("no context has been set");
		}
		return this.context;
	}

	protected ConfigurableApplicationContext getRequiredRefreshedContext() {
		final ConfigurableApplicationContext context = this.getRequiredContext();
		if (!context.isActive()) {
			context.refresh();
		}
		return context;
	}
}
