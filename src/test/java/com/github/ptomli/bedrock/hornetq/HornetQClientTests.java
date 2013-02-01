package com.github.ptomli.bedrock.hornetq;

import static org.fest.assertions.Assertions.*;

import org.hornetq.jms.client.HornetQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HornetQClientTests {

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testConnectionFactory() {
		assertThat(ctx.getBean("cf", HornetQConnectionFactory.class)).isNotNull();
	}
}
