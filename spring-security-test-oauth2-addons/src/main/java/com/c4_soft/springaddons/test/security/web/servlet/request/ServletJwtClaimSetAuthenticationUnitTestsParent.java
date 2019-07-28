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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.c4_soft.oauth2.rfc7519.JwtClaimSet;
import com.c4_soft.springaddons.test.security.support.Defaults;
import com.c4_soft.springaddons.test.security.support.jwt.JwtClaimSetAuthenticationRequestPostProcessor;
import com.c4_soft.springaddons.test.security.web.servlet.request.ServletJwtClaimSetAuthenticationUnitTestsParent.UnitTestConfig;
import com.c4_soft.springaddons.test.web.servlet.MockMvcSupport;

/**
 * <p>Parent class for <b>servlet</b> {@code @Controller} unit tests.</p>
 * <p>It provides with some tooling to create mocked {@code OAuth2ClaimSetAuthentication<JwtClaimSet>>},
 * a factory for {@link MockMvcSupport} and required test configuration</p>
 * <p>Providing {@code JwtDecoder}, {@code Converter<JwtClaimSet, Set<GrantedAuthority>>} or {@code JwtClaimSetAuthenticationRequestPostProcessor}
 * bean in a configuration of your own would be enough for those proposed in {@link UnitTestConfig} to back-off</p>
 *
 * @see com.c4_soft.springaddons.test.security.web.reactive.server.ReactiveJwtClaimSetAuthenticationUnitTestsParent reactive counterpart
 *
 * @author Jérôme Wacongne &lt;ch4mp&#64;c4-soft.com&gt;
 */
@Import(UnitTestConfig.class)
public abstract class ServletJwtClaimSetAuthenticationUnitTestsParent extends ServletUnitTestParent {

	/**
	 * @return a pre-configured {@link RequestPostProcessor} inject a mocked {@code OAuth2ClaimSetAuthentication<JwtClaimSet>}
	 * in test security context
	 */
	public JwtClaimSetAuthenticationRequestPostProcessor authentication() {
		return beanFactory.getBean(JwtClaimSetAuthenticationRequestPostProcessor.class);
	}

	/**
	 * @param claimsConsumer {@link Consumer} to configure JWT claim-set
	 * @return a pre-configured {@link RequestPostProcessor} inject a mocked {@code OAuth2ClaimSetAuthentication<JwtClaimSet>}
	 * in test security context
	 */
	public JwtClaimSetAuthenticationRequestPostProcessor authentication(Consumer<JwtClaimSet.Builder<?>> claimsConsumer) {
		final var requestPostProcessor = authentication();
		requestPostProcessor.claims(claimsConsumer);
		return requestPostProcessor;
	}

	@TestConfiguration
	public static class UnitTestConfig {

		@ConditionalOnMissingBean
		@Bean
		public JwtDecoder jwtDecoder() {
			return mock(JwtDecoder.class);
		}

		@ConditionalOnMissingBean
		@Bean
		@Scope("prototype")
		public Converter<JwtClaimSet, Set<GrantedAuthority>> authoritiesConverter() {
			final var mockAuthoritiesConverter = mock(JwtClaimSet2AuthoritiesConverter.class);

			when(mockAuthoritiesConverter.convert(any())).thenReturn(Defaults.GRANTED_AUTHORITIES);

			return mockAuthoritiesConverter;
		}

		@Bean
		@Scope("prototype")
		public JwtClaimSetAuthenticationRequestPostProcessor jwtClaimSetAuthenticationRequestPostProcessor(
				Converter<JwtClaimSet, Set<GrantedAuthority>> authoritiesConverter) {
			return new JwtClaimSetAuthenticationRequestPostProcessor(authoritiesConverter);
		}

		private static interface JwtClaimSet2AuthoritiesConverter extends Converter<JwtClaimSet, Set<GrantedAuthority>> {
		}
	}

}