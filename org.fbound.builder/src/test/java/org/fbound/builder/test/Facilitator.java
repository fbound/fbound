package org.fbound.builder.test;

import org.fbound.builder.BuilderOpts;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Facilitator extends User {
	public Facilitator(Record record) {
		super(record);
	}

	public static class Record extends User.Record {}

	public static class Builder extends RecordBuilder<Record,Facilitator,Facilitator,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Facilitator::new)); }
		public Builder() { this(new Record()); }
		public static class Fluent<R> extends RecordBuilder<Record,Record,R, Fluent<R>> {
			public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.build(new Record()).asFluent(consumer, returnRef)); }
		}
	}

	public static class RecordBuilder<T extends Record,V,R,B extends RecordBuilder<T,V,R,? super B>> extends User.RecordBuilder<T,V,R,B> {
		public RecordBuilder(BuilderOpts<T,V,R> options) { super(options); }
	}
}
