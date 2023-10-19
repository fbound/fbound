/*
 * Copyright 2023 Matthew Stevenson <fbound.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fbound.builder;

import org.fbound.builder.extension.Extension;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Matt Stevenson [matt@fbound.org]
 *
 * Utilities for Extension interfaces to access protected BuilderBase fields
 */
public class ExtensionUtils {
	public static <B extends BuilderBase<?, ?, ?, B> & Extension<B>> B self(Extension<B> extension) {
		return ((B) extension).self;
	}

	public static <T,B extends BuilderBase<T, ?, ?, B> & Extension<B>> T instance(Extension<B> extension) {
		return ((B) extension).buildRef.get();
	}

	public static <V,B extends BuilderBase<?, V, ?, B> & Extension<B>> V builtValue(Extension<B> extension) {
		return ((B) extension).builtValue;
	}

	public static <R,B extends BuilderBase<?, ?, R, B> & Extension<B>> R returnValue(Extension<B> extension) {
		return ((B) extension).returnRef.get();
	}

	public static <R,B extends BuilderBase<?, ?, R, B> & Extension<B>> R finalizeInstance(Extension<B> extension) {
		return ((B) extension).finalizeInstance();
	}

	public static <V,B extends BuilderBase<?, V, ?, B> & Extension<B>> Consumer<V> consumer(Extension<B> extension) {
		return ((B) extension).consumer;
	}

	public static <T,V,B extends BuilderBase<T, V, ?, B> & Extension<B>> Function<T,V> factory(Extension<B> extension) {
		return ((B) extension).factory;
	}
}
