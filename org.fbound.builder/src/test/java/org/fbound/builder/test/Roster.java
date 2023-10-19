package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;
import org.fbound.builder.extension.Accept;
import org.fbound.builder.extension.Apply;
import org.fbound.builder.extension.Edit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Roster {
    private String name;
    private Facilitator facilitator;
    private List<Membership> memberships;

	public Roster(Record record) {
		this.name = record.name;
		this.facilitator = new Facilitator.Builder(record.facilitator).finalizeUser();
		this.memberships = record.memberships.stream().map(Membership::new).collect(Collectors.toList());
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
	}

	public static class RecordBuilder<V,R> extends BuilderBase<Record,V,R,RecordBuilder<V,R>>
			implements Accept<RecordBuilder<V,R>>, Apply<RecordBuilder<V,R>>, Edit<Record,RecordBuilder<V,R>> {
		public RecordBuilder(BuilderOpts<Record,V,R> options) { super(options); }

		public RecordBuilder<V,R> setName(String name) {
	        buildRef.get().name = name;
	        return this;
	    }

	    public Facilitator.Builder.Fluent<RecordBuilder<V,R>> buildFacilitator() {
		    return new Facilitator.Builder.Fluent<>(r -> buildRef.get().facilitator = r, () -> self);
	    }

	    public Membership.RecordBuilder<RecordBuilder<V,R>>.MembershipBuilderGroup addMembership(){
	        return Membership.RecordBuilder.start(m -> buildRef.get().memberships.add(m), self);
	    }

		public R finalizeRoster(){
			return super.finalizeInstance();
		}
	}

	public static class Builder extends RecordBuilder<Roster,Roster> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Roster::new)); }
		public Builder() { this(new Record()); }
	}
}
