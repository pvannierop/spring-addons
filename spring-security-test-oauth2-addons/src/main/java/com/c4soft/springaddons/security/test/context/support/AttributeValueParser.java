/*
 * Copyright 2019 Jérôme Wacongne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.c4soft.springaddons.security.test.context.support;

/**
 *
 * @author Jérôme Wacongne &lt;ch4mp&#64;c4-soft.com&gt;
 *
 * @param <FROM_TYPE> type to parse (source)
 * @param <TO_TYPE> type after parsing (target)
 *
 */
public interface AttributeValueParser<FROM_TYPE, TO_TYPE> {

	/**
	 * @param value to de-serialize
	 * @return an Object
	 */
	TO_TYPE parse(FROM_TYPE value);
}