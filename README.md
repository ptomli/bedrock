# A Solid Foundation

[DropWizard](http://dropwizard.io) provides a fantastic base on which you can
build REST based services. Big up to [@codahale](http://github.com/codahale)
and [Yammer](https://www.yammer.com/) for making this available.

Bedrock takes this basis, adds some libraries and a bit more glue, and tries to
make it easy to build services using the technologies I commonly use.

## Bonus Features

 *  [Spring](http://projects.spring.io/spring-framework/) for wirin'.
 *  [Spring Integration](http://projects.spring.io/spring-integration/) for eipin'.
 *  [Spring Security](http://projects.spring.io/spring-security/) for securin'.
 *  [HornetQ](http://www.jboss.org/hornetq JMS client) for messagin'.

## Getting Started

```xml
	<dependency>
		<groupId>com.github.ptomli.bedrock</groupId>
		<artifactId>bedrock-core</artifactId>
		<version>2.0.0-SNAPSHOT</version>
	</dependency>
```

## Simple Example

Below is a simple example of how you can easily integrate Spring configured
beans into the DropWizard startup.

The code below:

 *  Creates a ClassPathXmlApplicationContext, based on the Spring bean
    configuration files located in `/META-INF/spring/*.xml`.
 *  Registers a Spring PropertySource with the Spring Environment, whose
    property values are resolved against the DropWizard configuration,
    prefixed with "config.".
 *  Registers the DropWizard configuration as a Spring bean named "config".
 *  Registers any HealthCheck beans, defined in Spring, with the DropWizard
    environment.
 *  Registers any @Path annotated beans, defined in Spring, with the DropWizard
    environment.

```java
    @Override
    public void run(Configuration configuration, Environment environment) {
        SpringServiceConfigurer.forEnvironment(environment)
            .withContext(ClassPathXmlApplicationContext.class, "classpath:/META-INF/spring/*.xml")
            .registerConfigurationPropertySource("config.", configuration)
            .registerConfigurationBean("config", configuration)
            .registerHealthChecks()
            .registerResources();
    }
```

## Testing Dependencies

I'm a strong believer that consistency makes it a lot easier to deal with code.
Testing is one of those areas where there tends to be, let's say, less rigorous
attention to consistency. Having a good set of testing tools available, and
no needing to go looking for a doodad or wotsit, tends to help keep things in
line.

 *  Spring Test
 *  Spring Integration Test
 *  AssertJ
 *  Mockito

# Build Status

[![Build Status](https://travis-ci.org/ptomli/bedrock.png?branch=master)](https://travis-ci.org/ptomli/bedrock)
[![Coverage Status](https://coveralls.io/repos/ptomli/bedrock/badge.png)](https://coveralls.io/r/ptomli/bedrock)

# License

Copyright 2013-2014 Paul Tomlin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
