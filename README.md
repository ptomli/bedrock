# A Solid Foundation

[DropWizard](http://github.com/codahale/dropwizard) provides a fantastic base
on which you can build REST based services. Big up to @codahale and
[Yammer](https://www.yammer.com/) for making this available.

Bedrock takes this basis, adds some libraries and a bit more glue, and tries to
make it easy to build services using the technologies I commonly use.

## Bonus Features

 *  [Spring](http://www.springsource.org/spring-framework) for wirin'.
 *  [Spring Integration](http://www.springsource.org/spring-integration) for eapin'.
 *  [Spring Security](http://www.springsource.org/spring-security) for securin'.
 *  [HornetQ](http://www.jboss.org/hornetq JMS client) for messagin'.

## Testing Dependencies

I'm a strong believer that consistency makes it a lot easier to deal with code.
Testing is one of those areas where there tends to be, let's say, less rigorous
attention to consistency. Having a good set of testing tools available, and
no needing to go looking for a doodad or wotsit, tends to help keep things in
line.

 *  Spring Test
 *  Spring Integration Test
 *  FEST Assertions
 *  Mockito

# Build Status

[![Build Status](https://travis-ci.org/ptomli/bedrock.png?branch=master)](https://travis-ci.org/ptomli/bedrock)

# License

Copyright 2013 Paul Tomlin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
