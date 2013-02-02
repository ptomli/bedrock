package com.github.ptomli.bedrock.spring;

import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.env.PropertySource;

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
 *     SpringServiceConfigurer.forContext(ClassPathXmlApplicationContext.class, "classpath:/META-INF/spring/applicationContext.xml")
 *         .registerConfigurationPropertySource("dw.", configuration)
 *         .registerConfigurationBean("dw", configuration)
 *         .registerResources(environment);
 * }
 * </code>
 * </pre>
 * 
 * @param <T> the ConfigurableApplicationContext type
 */
public class SpringServiceConfigurer<T extends ConfigurableApplicationContext> {
	private static final Logger LOG = LoggerFactory.getLogger(SpringServiceConfigurer.class);

	/**
	 * Create a new configurer with the provided ApplicationContext.
	 * 
	 * @param context
	 * @return the configurer
	 */
	public static <T extends ConfigurableApplicationContext> SpringServiceConfigurer<T> forContext(T context) {
		return new SpringServiceConfigurer<T>(context);
	}

	/**
	 * Create a new configurer which will instantiate an XML based
	 * ApplicationContext, using the provided configuration
	 * locations.
	 * 
	 * @param clazz the ApplicationContext class to instantiate
	 * @param configurations the configuration locations
	 * @return the configurer
	 */
	public static <T extends AbstractXmlApplicationContext> SpringServiceConfigurer<T> forContext(Class<T> clazz, String... configurations) {
		try {
			final T context = clazz.newInstance();
			context.setConfigLocations(configurations);
			return SpringServiceConfigurer.forContext(context);
		}
		catch (IllegalAccessException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
		catch (InstantiationException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
	}

	/**
	 * Create a new configurer which will instantiate an annotation based
	 * ApplicationContext, using the provided configuration classes.
	 * 
	 * @param clazz the ApplicationContext class to instantiate
	 * @param configurations the configuration classes
	 * @return the configurer
	 */
	public static <T extends AnnotationConfigApplicationContext> SpringServiceConfigurer<T> forContext(Class<T> clazz, Class<?>... configurations) {
		try {
			final T context = clazz.newInstance();
			context.register(configurations);
			return SpringServiceConfigurer.forContext(context);
		}
		catch (IllegalAccessException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
		catch (InstantiationException ex) {
			throw new ApplicationContextInstantiationException(ex);
		}
	}

	private T context;

	private SpringServiceConfigurer(T context) {
		this.context = context;
	}

	/**
	 * Return the ApplicationContext.
	 * 
	 * @return the ApplicationContext
	 */
	public T getApplicationContext() {
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
	 * @throws IllegalStateException if the context has already been refreshed
	 */
	public SpringServiceConfigurer<T> registerConfigurationPropertySource(final String prefix, final Configuration configuration) {
		if (this.context.isActive()) {
			throw new IllegalStateException("cannot register a property source after the context has been refreshed");
		}

		final PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(configuration);
		final PropertySource<?> propertySource = new PropertyAccessorPropertySource("dropwizard-config", prefix, accessor);
		this.context.getEnvironment()
		            .getPropertySources()
		            .addFirst(propertySource);
		return this;
	}

	/**
	 * Register the configuration instance as a Spring bean, using the provided
	 * name as the bean name.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @param name the name of the bean in the Spring context
	 * @param configuration the bean to register
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerConfigurationBean(final String name, final Configuration configuration) {
		this.refreshContext();
		context.getBeanFactory().registerSingleton(name, configuration);
		return this;
	}

	/**
	 * Registers HealthCheck beans defined in the application context with
	 * the environment.
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerHealthChecks(Environment environment) {
		this.refreshContext();
		final Map<String,HealthCheck> beans = this.context.getBeansOfType(HealthCheck.class);
		for (Map.Entry<String,HealthCheck> entry : beans.entrySet()) {
			LOG.info("registering HealthCheck: {}", entry.getValue());
			environment.addHealthCheck(entry.getValue());
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
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerProviders(Environment environment) {
		this.refreshContext();
		final Map<String, Object> beans = this.context.getBeansWithAnnotation(Provider.class);
		for (Map.Entry<String,Object> entry : beans.entrySet()) {
			LOG.info("registering @Provider: {}", entry.getValue());
			environment.addProvider(entry.getValue());
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
	 * @param environment
	 * @return this configurer
	 */
	@SuppressWarnings("rawtypes")
	public SpringServiceConfigurer<T> registerInjectableProviders(Environment environment) {
		this.refreshContext();
		final Map<String,InjectableProvider> beans = this.context.getBeansOfType(InjectableProvider.class);
		for (Map.Entry<String,InjectableProvider> entry : beans.entrySet()) {
			LOG.info("registering InjectableProvider: {}", entry.getValue());
			environment.addProvider(entry.getValue());
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
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerResources(Environment environment) {
		this.refreshContext();
		final Map<String,Object> beans = this.context.getBeansWithAnnotation(Path.class);
		for (Map.Entry<String,Object> entry : beans.entrySet()) {
			LOG.info("registering @Path resource: {}", entry.getValue());
			environment.addResource(entry.getValue());
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
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerTasks(Environment environment) {
		this.refreshContext();
		final Map<String,Task> beans = this.context.getBeansOfType(Task.class);
		for (Map.Entry<String,Task> entry : beans.entrySet()) {
			LOG.info("registering Task: {}", entry.getValue());
			environment.addTask(entry.getValue());
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
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerManaged(Environment environment) {
		this.refreshContext();
		final Map<String,Managed> beans = this.context.getBeansOfType(Managed.class);
		for (Map.Entry<String,Managed> entry : beans.entrySet()) {
			LOG.info("registering Managed: {}", entry.getValue());
			environment.manage(entry.getValue());
		}
		return this;
	}

	/**
	 * <p>
	 * Calling this method will refresh the context if it hasn't already been
	 * refreshed.
	 * 
	 * @param environment
	 * @return this configurer
	 */
	public SpringServiceConfigurer<T> registerLifeCycles(Environment environment) {
		this.refreshContext();
		final Map<String,LifeCycle> beans = this.context.getBeansOfType(LifeCycle.class);
		for (Map.Entry<String,LifeCycle> entry : beans.entrySet()) {
			LOG.info("registering LifeCycle: {}", entry.getValue());
			environment.manage(entry.getValue());
		}
		return this;
	}

	private void refreshContext() {
		if (!this.context.isActive()) {
			this.context.refresh();
		}
	}
}
