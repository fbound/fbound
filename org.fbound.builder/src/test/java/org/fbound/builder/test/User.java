package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class User {
	private String name;
	private Date date;
	private Map<String,String> opts;
	private Membership primaryMembership;
	private Membership secondaryMembership;

	public User(Record record){
		this.name = record.name;
		this.date = record.date;
		this.primaryMembership = new Membership.Builder(record.primaryMembership).finalizeMembership();
		this.secondaryMembership = new Membership.Builder(record.secondaryMembership).finalizeMembership();
		this.opts = record.opts;
	}

	public String getName() {
	    return name;
	}

	public Date getDate() {
	    return date;
	}

	public String getOption(String key) {
	    return opts.get(key);
	}

	public Membership getPrimaryMembership() {
	    return primaryMembership;
	}

	public Membership getSecondaryMembership() {
	    return secondaryMembership;
	}

	public static class Record {
		public String name;
		public Date date;
		public Map<String,String> opts = new HashMap<>();
		public Membership.Record primaryMembership;
		public Membership.Record secondaryMembership;
	}

	public static class RecordBuilder<T extends Record, V, R, B extends RecordBuilder<T,V,R,? super B>> extends BuilderBase<T,V,R,B> {
		public RecordBuilder(BuilderOpts<T,V,R> options) { super(options); }

		public B setName(String name) {
			instanceRef.get().name = name;
			return self;
		}

		public B setDate(Date date) {
			instanceRef.get().date = date;
			return self;
		}

		public B setOption(String key, String value){
			instanceRef.get().opts.put(key, value);
			return self;
		}

		public Membership.Builder.Fluent<B>.MembershipBuilderGroup setPrimaryMembership(){
			return Membership.Builder.Fluent.start(m -> instanceRef.get().primaryMembership = m, () -> self);
		}

		public Membership.Builder.Fluent<B>.MembershipBuilderGroup setSecondaryMembership(){
			return Membership.Builder.Fluent.start(m -> instanceRef.get().secondaryMembership = m, () -> self);
		}

		public R finalizeUser(){
			return super.finalizeInstance();
		}
	}

	public static class Builder extends RecordBuilder<Record,User,User,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(User::new)); }
		public Builder() { this(new Record()); }
		public static class Fluent<R> extends RecordBuilder<Record,Record,R, Fluent<R>> {
			public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.build(new Record()).asFluent(consumer, returnRef)); }
		}
	}
}
