package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;
import org.fbound.builder.extension.Accept;
import org.fbound.builder.extension.Apply;
import org.fbound.builder.extension.Edit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Roster {
    private String name;
    private Facilitator facilitator;
    private List<Membership> memberships;

	public Roster(Record record) {
		this.name = record.name;
		this.facilitator = new Facilitator.Builder(record.facilitator).finalizeUser();
		this.memberships = record.memberships.stream().map(r -> new Membership.Builder(r).finalizeMembership()).collect(Collectors.toList());
	}

	public String getName() {
        return name;
    }

    public Facilitator getFacilitator() {
        return facilitator;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

	public static class Record {
		public String name;
		public Facilitator.Record facilitator;
		public List<Membership.Record> memberships = new ArrayList<>();

		public static class Builder extends BaseBuilder<Record,Record> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder() { this(new Record()); }
			public static class Fluent<R> extends BaseBuilder<Record,R> {
				public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
			}
		}
	}

	public static class BaseBuilder<V,R> extends BuilderBase<Record,V,R, BaseBuilder<V,R>>
			implements Accept<BaseBuilder<V,R>>, Apply<BaseBuilder<V,R>>, Edit<Record, BaseBuilder<V,R>> {
		public BaseBuilder(BuilderOpts<Record,V,R> options) { super(options); }

		public BaseBuilder<V,R> setName(String name) {
	        instanceRef.get().name = name;
	        return this;
	    }

	    public Facilitator.Record.Builder.Fluent<BaseBuilder<V,R>> buildFacilitator() {
		    return new Facilitator.Record.Builder.Fluent<>(r -> instanceRef.get().facilitator = r, () -> self);
	    }

	    public Membership.Record.Builder.Fluent<BaseBuilder<V,R>>.MembershipBuilderGroup addMembership(){
	        return Membership.Record.Builder.Fluent.start(m -> instanceRef.get().memberships.add(m), () -> self);
	    }

		public R finalizeRoster(){
			return super.finalizeInstance();
		}
	}

	public static class Builder extends BaseBuilder<Roster,Roster> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Roster::new)); }
		public Builder() { this(new Record()); }
		public static class Fluent<R> extends BaseBuilder<Roster,R> {
			public Fluent(Consumer<Roster> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
		}
	}
}
