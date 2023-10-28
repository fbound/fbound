package org.fbound.builder.test;

import org.fbound.builder.BuilderOpts;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Facilitator extends User {
	/* New Facilitator fields go here */

	public Facilitator(Record record) {
		super(record);
		/* Set Facilitator fields from Facilitator.Record here */
	}

	/* New Facilitator methods go here */

	public static class Record extends User.Record {
		/* New Facilitator Record fields go here */

		public static class Builder extends BaseBuilder<Record,Record,Record, Builder> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder() { this(new Record()); }
			public static class Fluent<R> extends BaseBuilder<Record,Record,R, Builder.Fluent<R>> {
				public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
			}
		}
	}

	public static class BaseBuilder<T extends Record,V,R,B extends BaseBuilder<T,V,R,? super B>> extends User.BaseBuilder<T,V,R,B> {
		public BaseBuilder(BuilderOpts<T,V,R> options) { super(options); }
		/* New Facilitator Builder methods go here */
	}

	public static class Builder extends BaseBuilder<Record,Facilitator,Facilitator,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Facilitator::new)); }
		public Builder() { this(new Record()); }
		public static class Fluent<R> extends BaseBuilder<Record,Facilitator,R,Fluent<R>> {
			public Fluent(Consumer<Facilitator> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
		}
	}
}
