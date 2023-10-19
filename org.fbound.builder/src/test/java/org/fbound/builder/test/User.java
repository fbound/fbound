package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class User {
	private String name;
    private Date date;
    private Map<String,String> opts;
    private Membership primaryMembership;
    private Membership secondaryMembership;
    private List<Membership> memberships;

	public User(Record record){
		this.name = record.name;
		this.date = record.date;
		this.primaryMembership = new Membership(record.primaryMembership);
		this.secondaryMembership = new Membership(record.secondaryMembership);
		this.memberships = record.memberships.stream().map(Membership::new).collect(Collectors.toList());
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

    public List<Membership> getMemberships() {
        return memberships;
    }

	public static class Record {
		public String name;
		public Date date;
		public Map<String,String> opts = new HashMap<>();
		public Membership.Record primaryMembership;
		public Membership.Record secondaryMembership;
		public List<Membership.Record> memberships = new ArrayList<>();
	}

	public static class RecordBuilder<T extends Record, V, R, B extends RecordBuilder<T,V,R,? super B>> extends BuilderBase<T,V,R,B> {
		public RecordBuilder(BuilderOpts<T,V,R> options) { super(options); }

		public B setName(String name) {
			buildRef.get().name = name;
			return self;
		}

		public B setDate(Date date) {
			buildRef.get().date = date;
			return self;
		}

		public B setOption(String key, String value){
			buildRef.get().opts.put(key, value);
			return self;
		}

		public Membership.RecordBuilder<B>.MembershipBuilderGroup setPrimaryMembership(){
			return Membership.RecordBuilder.start(m -> buildRef.get().primaryMembership = m, self);
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
