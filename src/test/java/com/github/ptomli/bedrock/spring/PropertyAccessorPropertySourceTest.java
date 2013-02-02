package com.github.ptomli.bedrock.spring;

import static org.mockito.Mockito.*;
import static org.fest.assertions.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.PropertyAccessor;

public class PropertyAccessorPropertySourceTest {

	private PropertyAccessor accessor;

	@Before
	public void setup() {
		accessor = mock(PropertyAccessor.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullNameThrowsException() {
		new PropertyAccessorPropertySource(null, "X", accessor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyNameThrowsException() {
		new PropertyAccessorPropertySource("", "X", accessor);
	}

	@Test
	public void testNameIsPropagated() {
		assertThat(new PropertyAccessorPropertySource("name", "X", accessor).getName()).isEqualTo("name");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyPrefixThrowsException() {
		new PropertyAccessorPropertySource("name", "", accessor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullPrefixThrowsException() {
		new PropertyAccessorPropertySource("name", null, accessor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullAccessorThrowsException() {
		new PropertyAccessorPropertySource("name", "X", null);
	}

	@Test
	public void testValueIsNullWithWrongPrefix() {
		assertThat(new PropertyAccessorPropertySource("name", "dw.", accessor).getProperty("foo")).isEqualTo(null);
	}

	@Test
	public void testValueIsNullWithUnreadableProperty() {
		when(accessor.isReadableProperty("dw.foo")).thenReturn(true);
		assertThat(new PropertyAccessorPropertySource("name", "dw.", accessor).getProperty("foo")).isEqualTo(null);
	}

	@Test
	public void testValue() {
		when(accessor.isReadableProperty("foo")).thenReturn(true);
		when(accessor.getPropertyValue("foo")).thenReturn("foo");
		assertThat(new PropertyAccessorPropertySource("name", "dw.", accessor).getProperty("dw.foo")).isEqualTo("foo");
	}
}
