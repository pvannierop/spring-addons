/*
 * Copyright 2018 Jérôme Wacongne.
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
package com.c4_soft.springaddons.test.web.servlet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;

import com.c4_soft.springaddons.test.web.support.ByteArrayHttpOutputMessage;
import com.c4_soft.springaddons.test.web.support.SerializationHelper;

/**
 * <p>
 * Just another wrapper for Spring {@link MockMvc MockMvc}.<br>
 * It would extend {@link MockMvc} if it was not final :-/
 * </p>
 * Highlighted features:
 * <ul>
 * <li>auto sets "Accept" and "Content-Type" headers according to http method and body content type</li>
 * <li>serializes request body according to Content-type and registered message converters</li>
 * <li>provides with shortcuts to issue requests in basic but most common cases (no fancy headers, cookies, etc): get, post, patch, put
 * and delete methods</li>
 * <li>wraps MockMvc
 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
 * perform} and exposes request builder helpers for advanced cases (when you need to further customize
 * {@link org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
 * MockHttpServletRequestBuilder})</li>
 * </ul>
 *
 * @author Jérôme Wacongne &lt;ch4mp&#64;c4-soft.com&gt;
 */
@TestComponent
@Scope("prototype")
public class MockMvcSupport {
	private final MockMvc mockMvc;

	private final SerializationHelper conv;

	private final MediaType defaultMediaType;

	private final List<RequestPostProcessor> postProcessors;

	/**
	 * @param mockMvc wrapped Spring MVC testing helper
	 * @param serializationHelper used to serialize payloads to requested {@code Content-type} using Spring registered
	 * message converters
	 * @param defaultMediaType media-type to be used ({@code Content-type} or {@code Accept} headers, and payload
	 * serialization), when not specified as argument of this helper methods.<br>
	 * Set with {@code com.c4-soft.springaddons.test.web.default-media-type} configuration property.<br>
	 * Defaulted to {@code application/json}.
	 */
	@Autowired
	public MockMvcSupport(
			MockMvc mockMvc,
			SerializationHelper serializationHelper,
			@Value("${com.c4-soft.springaddons.test.web.default-media-type:application/json}") MediaType defaultMediaType) {
		this.mockMvc = mockMvc;
		this.conv = serializationHelper;
		this.defaultMediaType = defaultMediaType;
		this.postProcessors = new ArrayList<>();
	}

	/**
	 * Factory for a generic {@link org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
	 * MockHttpServletRequestBuilder} with relevant "Accept" and "Content-Type" headers. You might prefer to use
	 * {@link #getRequestBuilder(MediaType, String, Object...) getRequestBuilder} or alike which go further with request
	 * pre-configuration or even {@link #get(MediaType, String, Object...) get},
	 * {@link #post(Object, String, Object...)} and so on which issue simple requests in one step.
	 *
	 * @param accept should be non-empty when issuing response with body (GET, POST, OPTION), none otherwise
	 * @param method whatever HTTP verb you need
	 * @param urlTemplate end-point to be requested
	 * @param uriVars end-point template placeholders values
	 * @return a request builder with minimal info you can tweak further: add headers, cookies, etc.
	 */
	public MockHttpServletRequestBuilder
			requestBuilder(Optional<MediaType> accept, HttpMethod method, String urlTemplate, Object... uriVars) {
		final MockHttpServletRequestBuilder builder = request(method, urlTemplate, uriVars);
		accept.ifPresent(builder::accept);
		return builder;
	}

	/**
	 * To be called with fully configured request builder (wraps MockMvc
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform}).
	 *
	 * @param request fully configured request
	 * @return API answer to be tested
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions perform(MockHttpServletRequestBuilder requestBuilder) throws Exception {
		postProcessors.forEach(requestBuilder::with);
		return mockMvc.perform(requestBuilder);
	}

	/* GET */
	/**
	 * Factory providing with a request builder to issue a GET request (with Accept header).
	 *
	 * @param accept determines request Accept header (and response body format)
	 * @param urlTemplate API end-point to call
	 * @param uriVars values to feed URL template placeholders
	 * @return a request builder to be further configured (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder getRequestBuilder(MediaType accept, String urlTemplate, Object... uriVars) {
		return requestBuilder(Optional.of(accept), HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * Factory providing with a request builder to issue a GET request (with Accept header defaulted to what this helper
	 * is constructed with).
	 *
	 * @param urlTemplate API end-point to call
	 * @param uriVars values to feed URL template placeholders
	 * @return a request builder to be further configured (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder getRequestBuilder(String urlTemplate, Object... uriVars) {
		return getRequestBuilder(defaultMediaType, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a GET request with minimal headers and submit it.
	 *
	 * @param accept determines request Accept header (and response body format)
	 * @param urlTemplate API endpoint to be requested
	 * @param uriVars values to replace endpoint placeholders with
	 * @return API response to test
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions get(MediaType accept, String urlTemplate, Object... uriVars) throws Exception {
		return perform(getRequestBuilder(accept, urlTemplate, uriVars));
	}

	/**
	 * Shortcut to create a builder for a GET request with minimal headers and submit it (Accept header defaulted to
	 * what this helper was constructed with).
	 *
	 * @param urlTemplate API endpoint to be requested
	 * @param uriVars values to replace endpoint placeholders with
	 * @return API response to test
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions get(String urlTemplate, Object... uriVars) throws Exception {
		return perform(getRequestBuilder(urlTemplate, uriVars));
	}

	/* POST */
	/**
	 * Factory for a POST request builder containing a body set to payload serialized in given media type (with adequate
	 * Content-type header).
	 *
	 * @param payload to be serialized as body in contentType format
	 * @param contentType format to be used for payload serialization
	 * @param accept how should the response body be serialized (if any)
	 * @param urlTemplate API end-point to be requested
	 * @param uriVars values to replace end-point placeholders with
	 * @param <T> payload type
	 * @return Request builder to further configure (cookies, additional headers, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder postRequestBuilder(
			T payload,
			MediaType contentType,
			MediaType accept,
			String urlTemplate,
			Object... uriVars) throws Exception {
		return feed(requestBuilder(Optional.of(accept), HttpMethod.POST, urlTemplate, uriVars), payload, contentType);
	}

	/**
	 * Factory for a POST request builder. Body is pre-set to payload. Both Content-type and Accept headers are set to
	 * default media-type.
	 *
	 * @param payload request body
	 * @param urlTemplate API end-point
	 * @param uriVars values ofr URL template placeholders
	 * @param <T> payload type
	 * @return Request builder to further configure (cookies, additional headers, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder postRequestBuilder(T payload, String urlTemplate, Object... uriVars)
			throws Exception {
		return postRequestBuilder(payload, defaultMediaType, defaultMediaType, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a POST request with provided payload as body, using given media-type for serialization (and
	 * Content-type header).
	 *
	 * @param payload POST request body
	 * @param contentType media type used to serialize payload and set Content-type header
	 * @param accept media-type to be set as Accept header (and response serialization)
	 * @param urlTemplate API end-point to be called
	 * @param uriVars values ofr URL template placeholders
	 * @param <T> payload type
	 * @return API response to test
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions
			post(T payload, MediaType contentType, MediaType accept, String urlTemplate, Object... uriVars)
					throws Exception {
		return perform(postRequestBuilder(payload, contentType, accept, urlTemplate, uriVars));
	}

	/**
	 * Shortcut to issue a POST request with provided payload as body, using default media-type for serialization (and
	 * Content-type header).
	 *
	 * @param payload POST request body
	 * @param urlTemplate API end-point to be called
	 * @param uriVars values ofr URL template placeholders
	 * @param <T> payload type
	 * @return API response to test
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions post(T payload, String urlTemplate, Object... uriVars) throws Exception {
		return perform(postRequestBuilder(payload, urlTemplate, uriVars));
	}

	/* PUT */
	/**
	 * Factory for a POST request builder containing a body.
	 *
	 * @param payload to be serialized as body in contentType format
	 * @param contentType format to be used for payload serialization
	 * @param urlTemplate API end-point to be requested
	 * @param uriVars values to replace end-point placeholders with
	 * @param <T> payload type
	 * @return Request builder to further configure (cookies, additional headers, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder
			putRequestBuilder(T payload, MediaType contentType, String urlTemplate, Object... uriVars)
					throws Exception {
		return feed(requestBuilder(Optional.empty(), HttpMethod.PUT, urlTemplate, uriVars), payload, contentType);
	}

	/**
	 * Factory for a POST request builder containing a body. Default media-type is used for payload serialization (and
	 * Content-type header).
	 *
	 * @param payload to be serialized as body in contentType format
	 * @param urlTemplate API end-point to be requested
	 * @param uriVars values to replace end-point placeholders with
	 * @param <T> payload type
	 * @return Request builder to further configure (cookies, additional headers, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder putRequestBuilder(T payload, String urlTemplate, Object... uriVars)
			throws Exception {
		return putRequestBuilder(payload, defaultMediaType, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a PUT request.
	 *
	 * @param payload request body
	 * @param contentType payload serialization media-type
	 * @param urlTemplate API end-point to request
	 * @param uriVars values to be used in end-point URL placehoders
	 * @param <T> payload type
	 * @return API response to be tested
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions put(T payload, MediaType contentType, String urlTemplate, Object... uriVars)
			throws Exception {
		return perform(putRequestBuilder(payload, contentType, urlTemplate, uriVars));
	}

	/**
	 * Shortcut to issue a PUT request (with default media-type as Content-type).
	 *
	 * @param payload request body
	 * @param urlTemplate API end-point to request
	 * @param uriVars values to be used in end-point URL placehoders
	 * @param <T> payload type
	 * @return API response to be tested
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions put(T payload, String urlTemplate, Object... uriVars) throws Exception {
		return perform(putRequestBuilder(payload, urlTemplate, uriVars));
	}

	/* PATCH */
	/**
	 * Factory for a patch request builder (with Content-type already set).
	 *
	 * @param payload request body
	 * @param contentType payload serialization format
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point placeholders
	 * @param <T> payload type
	 * @return request builder to further configure (additional headers, cookies, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder
			patchRequestBuilder(T payload, MediaType contentType, String urlTemplate, Object... uriVars)
					throws Exception {
		return feed(requestBuilder(Optional.empty(), HttpMethod.PATCH, urlTemplate, uriVars), payload, contentType);
	}

	/**
	 * Factory for a patch request builder (with Content-type set to default media-type).
	 *
	 * @param payload request body
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point placeholders
	 * @param <T> payload type
	 * @return request builder to further configure (additional headers, cookies, etc.)
	 * @throws Exception if payload serialization goes wrong
	 */
	public <T> MockHttpServletRequestBuilder patchRequestBuilder(T payload, String urlTemplate, Object... uriVars)
			throws Exception {
		return patchRequestBuilder(payload, defaultMediaType, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a patch request with Content-type header and a body.
	 *
	 * @param payload request body
	 * @param contentType to be used for payload serialization
	 * @param urlTemplate end-point URL
	 * @param uriVars values for end-point URL placeholders
	 * @param <T> payload type
	 * @return API response to be tested
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions patch(T payload, MediaType contentType, String urlTemplate, Object... uriVars)
			throws Exception {
		return perform(patchRequestBuilder(payload, contentType, urlTemplate, uriVars));
	}

	/**
	 * Shortcut to issue a patch request with Content-type header and a body (using default media-type).
	 *
	 * @param payload request body
	 * @param urlTemplate end-point URL
	 * @param uriVars values for end-point URL placeholders
	 * @param <T> payload type
	 * @return API response to be tested
	 * @throws Exception if payload serialization goes wrong or what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public <T> ResultActions patch(T payload, String urlTemplate, Object... uriVars) throws Exception {
		return perform(patchRequestBuilder(payload, urlTemplate, uriVars));
	}

	/* DELETE */
	/**
	 * Factory for a DELETE request builder.
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return request builder to further configure (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder deleteRequestBuilder(String urlTemplate, Object... uriVars) {
		return requestBuilder(Optional.empty(), HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a DELETE request (no header)
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return API response to be tested
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions delete(String urlTemplate, Object... uriVars) throws Exception {
		return perform(deleteRequestBuilder(urlTemplate, uriVars));
	}

	/* HEAD */
	/**
	 * Factory for a HEAD request builder.
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return request builder to further configure (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder headRequestBuilder(String urlTemplate, Object... uriVars) {
		return requestBuilder(Optional.empty(), HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue a HEAD request (no header)
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return API response to be tested
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions head(String urlTemplate, Object... uriVars) throws Exception {
		return perform(headRequestBuilder(urlTemplate, uriVars));
	}

	/* OPTION */
	/**
	 * Factory for an OPTION request initialized with an Accept header.
	 *
	 * @param accept response body media-type
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return request builder to be further configured (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder optionRequestBuilder(MediaType accept, String urlTemplate, Object... uriVars) {
		return requestBuilder(Optional.of(accept), HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * Factory for an OPTION request initialized with an Accept header set to default media-type.
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return request builder to be further configured (additional headers, cookies, etc.)
	 */
	public MockHttpServletRequestBuilder optionRequestBuilder(String urlTemplate, Object... uriVars) {
		return optionRequestBuilder(defaultMediaType, urlTemplate, uriVars);
	}

	/**
	 * Shortcut to issue an OPTION request with Accept header
	 *
	 * @param accept response body media-type
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return API response to be further configured
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions option(MediaType accept, String urlTemplate, Object... uriVars) throws Exception {
		return perform(optionRequestBuilder(accept, urlTemplate, uriVars));
	}

	/**
	 * Shortcut to issue an OPTION request with default media-type as Accept header
	 *
	 * @param urlTemplate API end-point
	 * @param uriVars values for end-point URL placeholders
	 * @return API response to be further configured
	 * @throws Exception what
	 * {@link org.springframework.test.web.servlet.MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)
	 * perform} throws
	 */
	public ResultActions option(String urlTemplate, Object... uriVars) throws Exception {
		return perform(optionRequestBuilder(urlTemplate, uriVars));
	}

	/**
	 * Adds serialized payload to request content. Rather low-level, consider using this class
	 * {@link org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder MockHttpServletRequestBuilder}
	 * factories instead (getRequestBuilder, postRequestBuilder, etc.)
	 *
	 * @param request builder you want to set body to
	 * @param payload object to be serialized as body
	 * @param mediaType what format you want payload to be serialized to (corresponding HttpMessageConverter must be
	 * registered)
	 * @param <T> payload type
	 * @return the request with provided payload as content
	 * @throws Exception if things go wrong (no registered serializer for payload type and asked MediaType,
	 * serialization failure, ...)
	 */
	public <T> MockHttpServletRequestBuilder feed(MockHttpServletRequestBuilder request, T payload, MediaType mediaType)
			throws Exception {
		if (payload == null) {
			return request;
		}

		final ByteArrayHttpOutputMessage msg = conv.outputMessage(payload, mediaType);
		return request.headers(msg.headers).content(msg.out.toByteArray());
	}

	public DispatcherServlet getDispatcherServlet() {
		return mockMvc.getDispatcherServlet();
	}

	/**
	 * @param postProcessor request post-processor to be added to the list of those applied before request is performed
	 * @return this {@link MockMvcSupport}
	 */
	public MockMvcSupport with(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "postProcessor is required");
		this.postProcessors.add(postProcessor);
		return this;
	}

}