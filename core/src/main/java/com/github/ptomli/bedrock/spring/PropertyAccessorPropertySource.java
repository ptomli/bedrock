package com.github.ptomli.bedrock.spring;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.env.PropertySource;

import com.google.common.base.Strings;

/**
 * A {@link PropertySource} which uses a {@link PropertyAccessor} to retrieve
 * the actual property values.
 * <p>
 * Properties accessible through this property source are prefixed with a
 * constant string. If the accessor has a property "foo", and the prefix
 * for the property source is "dw.", then the property name, as exposed
 * by this property source, is "dw.foo".
 */
class PropertyAccessorPropertySource extends PropertySource<PropertyAccessor> {

	private final String prefix;
	private final PropertyAccessor accessor;

	/**
	 * Create a new instance with the provided name, property name prefix and accessor.
	 * 
	 * @param name the name of the property source
	 * @param prefix the prefix to prepend to the property names
	 * @param accessor the accessor through which properties should be retrieved
	 */
	public PropertyAccessorPropertySource(final String name, final String prefix, final PropertyAccessor accessor) {
		super(name);

		if (Strings.isNullOrEmpty(prefix)) {
			throw new IllegalArgumentException("prefix may not be null or empty");
		}
		if (accessor == null) {
			throw new IllegalArgumentException("accessor may not be null");
		}

		this.prefix = prefix;
		this.accessor = accessor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(final String name) {
		if (!name.startsWith(this.prefix)) {
			return null;
		}
		final String property = name.substring(this.prefix.length());
		return this.accessor.isReadableProperty(property) ? this.accessor.getPropertyValue(property) : null;
	}

}
