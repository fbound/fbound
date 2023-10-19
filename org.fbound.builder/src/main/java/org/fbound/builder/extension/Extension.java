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

/**
 * @author Matt Stevenson [matt@fbound.org]
 *
 * Marker class for BuilderBase-bound Interfaces.  Allows use of ExtensionUtils to access protected BuilderBase fields.
 * Any Extension instance can be cast to B, the Extension's Builder type.
 * @param <B> Builder Self Type, the type of the Builder class extending BuilderBase.
 */
public interface Extension<B extends BuilderBase<?,?,?,B> & Extension<B>> { }
