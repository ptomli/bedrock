package com.github.ptomli.bedrock.spring;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Interface specifying Spring context configuration, to enable easier
 * context creation based on settings in the DropWizard configuration file.
 * <p>
 * @see DefaultSpringContextConfiguration
 * @see SpringServiceConfigurer#withContextConfiguration(SpringContextConfiguration)
 *
 */
public interface SpringContextConfiguration {
	/**
	 * Specifies the application context class.
	 * <p>
	 * This should be one of
	 * <li>a subclass of {@link AbstractRefreshableConfigApplicationContext}
	 * <li>{@link AnnotationConfigApplicationContext}, or a subclass thereof
	 * 
	 * @return the application context class
	 */
	Class<? extends ConfigurableApplicationContext> getApplicationContextClass();

	/**
	 * Specifies the locations used to configure the application context.
	 * <p>
	 * If {@link #getApplicationContextClass()} returns a subclass of
	 * {@link AbstractRefreshableConfigApplicationContext} then the locations
	 * returned here should be suitable for supplying to
	 * {@link AbstractRefreshableConfigApplicationContext#setConfigLocations(String[]) setConfigLocations}.
	 * <p>
	 * If {@link #getApplicationContextClass()} returns
	 * {@link AnnotationConfigApplicationContext} then the locations
	 * returned here should be either names of {@link Configuration} annotated
	 * classes, or packages to scan. Configuration locations will be registered,
	 * or scanned, in order, with determination of which process based on the
	 * existence of an annotated class of the given name.
	 * <p>
	 * Must never return null
	 * 
	 * @return the application context configuration locations
	 */
	String[] getConfigLocations();

	/**
	 * Specifies which profiles should be activated on the application context.
	 * <p>
	 * Must never return null
	 * 
	 * @return the profiles to activate on the application context
	 */
	String[] getProfiles();

	/**
	 * Specifies property source to be registered, in order, with the
	 * {@link ConfigurableEnvironment environment}.
	 * 
	 * @return the property sources to add to the application context
	 */
	PropertySource<?>[] getPropertySources();
}
