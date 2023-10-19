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
package org.fbound.builder.extension;

import org.fbound.builder.BuilderBase;

import java.util.function.Function;

import static org.fbound.builder.ExtensionUtils.self;

/**
 * @author Matt Stevenson [matt@fbound.org]
 *
 * @param <B>
 */
public interface Apply<B extends BuilderBase<?,?,?,B> & Apply<B>> extends Extension<B> {
	default public <T> T apply(Function<B,T> function) {
		return function.apply(self(this));
	}
}
