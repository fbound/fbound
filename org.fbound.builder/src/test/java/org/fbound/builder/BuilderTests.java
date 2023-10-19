package org.fbound.builder;

import org.fbound.builder.test.Roster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;

public class BuilderTests {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Before
	public void setup(){}

	private boolean assertException(Runnable test){
		log.info("Expecting exception:");
		try {
			test.run();
		}catch(Throwable t){
			return true;
		}
		Assert.fail("no exception caught");
		return false;
	}

	@Test
	public void testRosterBuilder() {
		String[] nameRef = {null};
		Roster r = new Roster.Builder()
				.setName("name")

				.buildFacilitator()
					.setName("facil name")
					.setDate(new Date())
					.finalizeUser()

				.addMembership().groupId("gid").cohortId("cid").membershipId("mid")

				.accept(BuilderTests::builderAction)
				.accept(b -> {
					try {
						Thread.sleep(10);
						System.out.println("Stopping to say hi!, " + b.toString());
					}catch (InterruptedException ignored){}
				})

				.apply(BuilderTests.RosterActions::new)
					.stayHereOn1().stayHereOn1()
					.goNextTo2().stayHereOn2().goBackTo1()
					.stayHereOn1().goNextTo2()
					.returnToBuilder()

				.edit(record -> {
					if(record.name.length() < 3){
						record.name = "100-" + record.name;
						nameRef[0] = record.name;
					}
				})

				.edit(record -> {
					if(nameRef[0] != null){
						record.name = record.name + "-2";
					}
				})

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