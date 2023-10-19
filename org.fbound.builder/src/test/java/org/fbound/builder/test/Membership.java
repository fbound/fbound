package org.fbound.builder.test;

import java.util.function.Consumer;

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
	}

	public static class RecordBuilder<R> {
	    public static <T> RecordBuilder<T>.MembershipBuilderGroup start(Consumer<Record> setter, T returnObject){
	        return new RecordBuilder<>(setter, returnObject).new MembershipBuilderGroup();
	    }

	    private Consumer<Record> setter;
	    private R returnObject;
	    private Record membership;

	    private RecordBuilder(Consumer<Record> setter, R returnObject) {
	        this.setter = setter;
	        this.returnObject = returnObject;
	        this.membership = new Record();
	    }

	    public class MembershipBuilderGroup {
	        public MembershipBuilderCohort groupId(String groupId){
	            membership.groupId = groupId;
	            return new MembershipBuilderCohort();
	        }
	    }

	    public class MembershipBuilderCohort {
	        public MembershipBuilderPerson cohortId(String cohortId){
	            membership.cohortId = cohortId;
	            return new MembershipBuilderPerson();
	        }
	    }

	    public class MembershipBuilderPerson {
	        public R membershipId(String membershipId){
	            membership.membershipId = membershipId;
	            setter.accept(membership);
	            return returnObject;
	        }
	    }
	}
}
