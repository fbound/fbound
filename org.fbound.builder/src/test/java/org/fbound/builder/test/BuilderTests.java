package org.fbound.builder.test;

import org.fbound.builder.BuilderOpts;
import java.util.Date;

public class BuilderTests {

	public static void main(String[] args) {
		// Using final Array to set/get String value from within lambdas.
		final String[] nameRef = {null};

		// Unguided Builder
		Membership m = new Membership.Builder().setCohortId("cid").setMembershipId("mid").setGroupId("gid").finalizeMembership();
		// Guided Builder
		Membership m2 = Membership.Builder.start().groupId("gid").cohortId("cid").membershipId("mid");
		// Guided Builder to String Value
		String rStr = Membership.RecordBuilder.start(BuilderOpts.build(new Membership.Record()).toValue(Object::toString)).groupId("gid").cohortId("cid").membershipId("mid");

		Roster r = new Roster.Builder()
				.setName("name")

				.buildFacilitator()// Change to Facilitator Builder
					.setName("facil name")
					.setDate(new Date())
					.finalizeUser() // return to Roster Builder

				// Use guided Membership Builder
				.addMembership().groupId("gid").cohortId("cid").membershipId("mid")

				// Accept used to pass in a method
				.accept(BuilderTests::builderAction)

				// Accept used as fluent escape-hatch
				.accept(b -> {
					try {
						Thread.sleep(10);
						System.out.println("Stopping to say hi!, " + b.toString());
					}catch (InterruptedException ignored){}
				})

				// Apply used to change to another class designed to return to Roster Builder
				.apply(BuilderTests.RosterActions::new)
					.stayHereOn1().stayHereOn1()
					.goNextTo2().stayHereOn2().goBackTo1()
					.stayHereOn1().goNextTo2()
					.returnToBuilder()

				// Edit used to copy name value into local variable
				.edit(record -> {
					if(record.name.length() < 3){
						record.name = "100-" + record.name;
						nameRef[0] = record.name;
					}
				})
				// Edit used to access local variable
				.edit(record -> record.name = nameRef[0] + "-2")

				.finalizeRoster();
	}

	public static void builderAction(Roster.RecordBuilder<?,?> b){
		b.addMembership().groupId("gid2").cohortId("cid2").membershipId("mid2");
	}

	public static class RosterActions<V,R,B extends Roster.RecordBuilder<V,R>> {
		private B builder;
		public RosterActions(B builder) {
			this.builder = builder;
		}

		public B goBackToBuilder() {
			return builder;
		}
		public RosterActions<V,R,B> stayHereOn1() {
			return this;
		}
		public RosterActions2<V,R,B> goNextTo2() {
			return new RosterActions2<>(builder);
		}
	}

	public static class RosterActions2<V,R,B extends Roster.RecordBuilder<V,R>> {
		private B builder;
		public RosterActions2(B builder) {
			this.builder = builder;
		}

		public RosterActions<V,R,B> goBackTo1() {
			return new RosterActions<>(builder);
		}
		public RosterActions2<V,R,B> stayHereOn2() {
			return this;
		}
		public B returnToBuilder() {
			return builder;
		}
	}
}