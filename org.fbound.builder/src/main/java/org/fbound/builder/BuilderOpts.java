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
 * Options needed to construct a Builder with a specific form.
 * @param <T> Instance Type of record that is modified by the Builder, typically a serializable Record type
 * @param <V> Value Type of final Object constructed from the T record instance
 * @param <R> Return Type of Object returned after final Value construction.  This will typically be V for a standalone Builder, or another Builder Type when chaining Builders.
 */
public class BuilderOpts<T,V,R> {
	final Supplier<T> supplier;
	final Function<T,V> factory;
	final Consumer<V> consumer;
	final Supplier<R> returnSupplier;
	final Consumer<BuilderBase<T,V,R,?>> builderSetup;

	private BuilderOpts() throws IllegalAccessException{
		throw new IllegalAccessException("Creating custom BuilderOpts not allowed.");
	}

	private BuilderOpts(Supplier<T> supplier, Function<T, V> factory, Consumer<V> consumer, Supplier<R> returnSupplier, Consumer<BuilderBase<T,V,R,?>> builderSetup) {
		this.supplier = supplier;
		this.factory = factory;
		this.consumer = consumer;
		this.returnSupplier = returnSupplier;
		this.builderSetup = builderSetup;
	}

	public final static <T> Record<T> build(Supplier<T> supplier) {
		return new Record<>(supplier);
	}

	public final static <T> Record<T> build(T record) {
		return build(() -> record);
	}

	public final static <T,V,R> FromBuilder<T,V,R> from(BuilderBase<T,V,R,?> builder) {
		return new FromBuilder<>(builder);
	}

	public final static class Record<T> extends BuilderOpts<T,T,T> {
		private Record(Supplier<T> supplier) {
			super(supplier, r->r, v->{}, supplier, b -> {});
		}

		public <V> Value<T,V> toValue(Function<T,V> factory){
			return new Value<>(supplier, factory);
		}

		public <R> BuilderOpts<T,T,R> asFluent(Consumer<T> consumer, Supplier<R> returnSupplier){
			return new BuilderOpts<>(supplier, r->r, consumer, returnSupplier, b -> {});
		}
	}

	public final static class Value<T,V> extends BuilderOpts<T,V,V> {
		public Value(Supplier<T> supplier, Function<T, V> factory) {
			super(supplier, factory, v->{}, null, b -> b.returnRef = () -> b.builtValue);
		}

		public <R> BuilderOpts<T,V,R> asFluent(Consumer<V> consumer, Supplier<R> returnSupplier){
			return new BuilderOpts<>(supplier, factory, consumer, returnSupplier, b -> {});
		}

		public <R> BuilderOpts<T,V,T> returnRecord(Consumer<V> consumer){
			return new BuilderOpts<>(supplier, factory, consumer, null, b -> b.returnRef = b.buildRef);
		}
	}

	public final static class FromBuilder<T,V,R> extends BuilderOpts<T,V,R> {
		public FromBuilder(BuilderBase<T,V,R,?> builder) {
			super(builder.buildRef, builder.factory, builder.consumer, builder.returnRef, b -> {});
		}
		private FromBuilder(Supplier<T> supplier, Function<T, V> factory, Consumer<V> consumer, Supplier<R> returnSupplier, Consumer<BuilderBase<T,V,R,?>> builderSetup) {
			super(supplier, factory, consumer, returnSupplier, builderSetup);
		}

		public Record<T> toRecord(){
			return new Record<>(supplier);
		}

		public <S> Value<T, S> toValue(Function<T, S> factory){
			return new Value<>(supplier, factory);
		}

		public <S> FromBuilder<T,S,R> toValue(Function<T,S> factory, Consumer<S> consumer){
			return new FromBuilder<>(supplier, factory, consumer, returnSupplier, b -> {});
		}

		public BuilderOpts<T,T,R> noValue(Consumer<T> consumer){
			return new FromBuilder<>(supplier, r->r, consumer, returnSupplier, b -> {});
		}

		public <S> FromBuilder<T,V,S> asFluent(Consumer<V> consumer, Supplier<S> returnSupplier){
			return new FromBuilder<>(supplier, factory, consumer, returnSupplier, b -> {});
		}

		public FromBuilder<T,V,V> notFluent(){
			return new FromBuilder<>(supplier, factory, v->{}, null, b -> b.returnRef = () -> b.builtValue);
		}
	}

	public final static class Other {
		public static <V, B extends BuilderBase<B,V,V,B>> BuilderOpts<B,V,V> effectiveBuilder(Function<B,V> factory){
			return new BuilderOpts<>( null, factory, v->{}, null, b -> {
					b.buildRef = () -> (B)b;
					b.returnRef = () -> b.builtValue;
			});
		}
	}
}