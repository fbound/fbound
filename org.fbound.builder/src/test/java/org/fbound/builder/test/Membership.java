package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Membership {
    String groupId;
    String cohortId;
    String membershipId;

	public Membership(Record record){
		if(record == null) return;
		this.groupId = record.groupId;
		this.cohortId = record.cohortId;
		this.membershipId = record.membershipId;
	}

    public String getGroupId() {
        return groupId;
    }

    public String getCohortId() {
        return cohortId;
    }

    public String getMembershipId() {
        return membershipId;
    }

	public static class Record {
	    public String groupId;
	    public String cohortId;
	    public String membershipId;

		public static class Builder extends BaseBuilder<Record,Record,Builder> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder() { this(new Record()); }
			public static class Fluent<R> extends BaseBuilder<Record,R,Fluent<R>> {
				public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
				public static <R> Fluent<R>.MembershipBuilderGroup start(Consumer<Record> consumer, Supplier<R> returnRef){
					return new Fluent<>(consumer, returnRef).new MembershipBuilderGroup();
				}
			}
		}
	}

	public static class BaseBuilder<V,R,B extends BaseBuilder<V,R,? super B>> extends BuilderBase<Record,V,R,B> {
		public static <V,R,B extends BaseBuilder<V,R,? super B>> B.MembershipBuilderGroup start(BuilderOpts<Record,V,R> options){
			 return ((B)new BaseBuilder<>(options)).new MembershipBuilderGroup();
		}

		private BaseBuilder(BuilderOpts<Record, V, R> options) {
			super(options);
		}

		public B setGroupId(String id){
			instanceRef.get().groupId = id;
			return self;
		}

		public B setCohortId(String id){
			instanceRef.get().cohortId = id;
			return self;
		}

		public B setMembershipId(String id){
			instanceRef.get().membershipId = id;
			return self;
		}

		public R finalizeMembership(){
			return super.finalizeInstance();
		}

		public class MembershipBuilderGroup {
	        public MembershipBuilderCohort groupId(String groupId){
	            BaseBuilder.this.setGroupId(groupId);
	            return new MembershipBuilderCohort();
	        }
	    }
	    public class MembershipBuilderCohort {
	        public MembershipBuilderPerson cohortId(String cohortId){
		        BaseBuilder.this.setCohortId(cohortId);
	            return new MembershipBuilderPerson();
	        }
	    }
	    public class MembershipBuilderPerson {
	        public R membershipId(String membershipId){
		        BaseBuilder.this.setMembershipId(membershipId);
	            return finalizeInstance();
	        }
	    }
	}

	public static class Builder extends BaseBuilder<Membership,Membership,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Membership::new)); }
		public Builder() { this(new Record()); }
		public static Builder.MembershipBuilderGroup start(){ return new Builder().new MembershipBuilderGroup(); }
		public static class Fluent<R> extends BaseBuilder<Membership,R,Fluent<R>> {
			public Fluent(Consumer<Membership> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
		}
	}
}
