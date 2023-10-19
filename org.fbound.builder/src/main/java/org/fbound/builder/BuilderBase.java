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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Matt Stevenson [matt@fbound.org]
 *
 * Base Builder type to be extended by other Builders.
 * Extending the BuilderBase allows using Builder Extension interfaces.
 * @param <T> Instance Type of record that is modified by the Builder, typically a serializable Record type
 * @param <V> Value Type of final Object constructed from the T record instance
 * @param <R> Return Type of Object returned after final Value construction. This will typically be V for a standalone Builder, or another Builder Type when chaining Builders.
 * @param <B> Builder Self Type, the type of the Builder class extending BuilderBase or a BuilderBase subtype
 */
public abstract class BuilderBase<T,V,R,B extends BuilderBase<? super T,V,R,? super B>> {
	/**
	 * This supplier is called EVERY time the instance is modified, the value is never cached, DO NOT USE a constructor or factory. This is a Supplier to reference an object that must be created after creating the Builder or to allow controlled access or proxy objects.
	 */
	protected Supplier<T> instanceRef;
	protected Function<T,V> factory;
	protected Consumer<V> consumer;
	protected Supplier<R> returnRef;

	protected V builtValue = null;
	/**
	 * Safely cast '(B)this' once to avoided multiple 'unchecked cast' warnings. Not final, can be set to wrap another Builder. Assign with caution.
	 */
	protected B self = (B) this;

	/**
	 * @param instanceRef Reference to the instance object to modify. This supplier is called every time the instance is modified, the value is never cached. Do not use a constructor or factory. This is a Supplier to reference an object that must be created after creating the Builder or to allow controlled access or proxy objects.
	 * @param factory Factory function to construct V 'builtValue' from T 'instanceRef.get()'
	 * @param consumer Consumer to use the final built V value, will be called once during 'finalizeInstance'
	 * @param returnRef Supplies the value to return for 'finalizeInstance'
	 */
	protected BuilderBase(Supplier<T> instanceRef, Function<T,V> factory, Consumer<V> consumer, Supplier<R> returnRef) {
		this.instanceRef = instanceRef;
		this.factory = factory;
		this.consumer = consumer;
		this.returnRef = returnRef;
	}

	protected BuilderBase(T instance, Function<T,V> factory, Consumer<V> consumer, Supplier<R> returnRef) {
		this(() -> instance, factory, consumer, returnRef);
	}

	protected BuilderBase(BuilderOpts<T,V,R> options) {
		this(options.supplier, options.factory, options.consumer, options.returnSupplier);
		options.builderSetup.accept(this);
	}

	/**
	 * Construct and set V 'builtValue' from T instance using 'factory'
	 * Passes 'builtValue' to 'consumer', then returns the value supplied by 'returnRef'.
	 * @return value supplied by 'returnRef', typically will be V 'builtValue' for standalone Builders or another Builder type when chaining Builders.
	 */
	protected R finalizeInstance() {
		builtValue = factory.apply(instanceRef.get());
		consumer.accept(builtValue);
		return returnRef.get();
	}
}
