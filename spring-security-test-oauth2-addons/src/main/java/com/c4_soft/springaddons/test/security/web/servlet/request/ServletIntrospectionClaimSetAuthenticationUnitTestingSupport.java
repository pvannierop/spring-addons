/*
 * Copyright 2019 Jérôme Wacongne
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.c4_soft.springaddons.test.security.web.servlet.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;

import com.c4_soft.oauth2.rfc7662.IntrospectionClaimSet;
import com.c4_soft.springaddons.test.security.support.Defaults;
import com.c4_soft.springaddons.test.security.support.introspection.IntrospectionClaimSetAuthenticationRequestPostProcessor;

/**
 * <p>A {@link ServletUnitTestingSupport} with additional helper methods to configure test {@code Authentication} instance,
 * it being an {@code OAuth2ClaimSetAuthentication<IntrospectionClaimSet>}.</p>
 *
 * Usage as test class parent:<pre>
 * &#64;RunWith( SpringRunner.class )
 * &#64;WebMvcTest( TestController.class )
 * public class TestControllerTests extends ServletIntrospectionClaimSetAuthenticationUnitTestingSupport {
 *
 *   &#64;Test
 *   public void testDemo() {
 *     mockMvc()
 *       .with(authentication().name("ch4mpy").authorities("message:read"))
 *       .get("/authentication")
 *       .expectStatus().isOk();
 *   }
 * }</pre>
 *
 * Same can be achieved using it as collaborator (note additional {@code @Import} statement):<pre>
 * &#64;RunWith( SpringRunner.class )
 * &#64;WebMvcTest( TestController.class )
 * &#64;Import( ServletIntrospectionClaimSetAuthenticationUnitTestingSupport.class )
 * public class TestControllerTests {
 *
 *   &#64;Autowired
 *   private ServletIntrospectionClaimSetAuthenticationUnitTestingSupport testingSupport;
 *
 *   &#64;Test
 *   public void testDemo() {
 *     testingSupport
 *       .mockMvc()
 *       .with(testingSupport.authentication().name("ch4mpy").authorities("message:read"))
 *       .get("/authentication")
 *       .expectStatus().isOk();
 *   }
 * }</pre>
 *
 * @author Jérôme Wacongne &lt;ch4mp&#64;c4-soft.com&gt;
 *
 */
@Import(ServletIntrospectionClaimSetAuthenticationUnitTestingSupport.UnitTestConfig.class)
public class ServletIntrospectionClaimSetAuthenticationUnitTestingSupport extends ServletUnitTestingSupport {

	public IntrospectionClaimSetAuthenticationRequestPostProcessor authentication() {
		return beanFactory.getBean(IntrospectionClaimSetAuthenticationRequestPostProcessor.class);
	}

	public IntrospectionClaimSetAuthenticationRequestPostProcessor authentication(
			Consumer<IntrospectionClaimSet.Builder<?>> claimsConsumer) {
		final var requestPostProcessor = beanFactory.getBean(IntrospectionClaimSetAuthenticationRequestPostProcessor.class);
		requestPostProcessor.claims(claimsConsumer);
		return requestPostProcessor;
	}

	@TestConfiguration
	public static class UnitTestConfig {

		@ConditionalOnMissingBean
		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public Converter<IntrospectionClaimSet, Set<GrantedAuthority>> authoritiesConverter() {
			final var mockAuthoritiesConverter = mock(IntrospectionClaimSet2AuthoritiesConverter.class);

			when(mockAuthoritiesConverter.convert(any())).thenReturn(Defaults.GRANTED_AUTHORITIES);

			return mockAuthoritiesConverter;
		}

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public IntrospectionClaimSetAuthenticationRequestPostProcessor introspectionClaimSetAuthenticationRequestPostProcessor(
				Converter<IntrospectionClaimSet, Set<GrantedAuthority>> authoritiesConverter) {
			return new IntrospectionClaimSetAuthenticationRequestPostProcessor(authoritiesConverter);
		}

		@Bean
		public ServletIntrospectionClaimSetAuthenticationUnitTestingSupport testingSupport() {
			return new ServletIntrospectionClaimSetAuthenticationUnitTestingSupport();
		}

		private static interface IntrospectionClaimSet2AuthoritiesConverter extends Converter<IntrospectionClaimSet, Set<GrantedAuthority>> {
		}
	}

}
