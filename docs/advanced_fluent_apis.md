# FBound Advanced Fluent APIs
```
Copyright 2023 FBound project <fbound.org>
Author: Matt Stevenson <matt@pagemodel.org>
```


This is a guide to Advanced Techniques for Generic Types and their practical use in software engineering.  All code in this guide is written in Java 8, you do not need to upgrade your Java to use these techniques. 

It will explore the design and type-parameters used in the FBound `BuilderBase<T,V,R,B extends BuilderBase<T,V,R,? super B>>`.  By the end of this guide, if I have done my job, you will understand why these are **fundamental type parameters** for generic classes.  Each with special properties that can combine to make a *more*-generic type with very interesting behaviors.

We will start with a concrete `UserBuilder` class loosely following the `Effective Builder Pattern` and explore its limitations.  
Each type parameter will be discussed independently as a means of overcoming a specific limitation of an `Effective Builder`.  
Each combination of type parameters will be examined to see how they interact with each other.  
Then, we will derive the `BuilderBase` class from the *generic* `UserBuilder` class.
To close, we'll have some fun with generic-type-bound interfaces and make something like`traits`/`mixins` in Java.

## Simple Fluent Builders

### StringBuilder
A simple Fluent Builder object is the Java `StringBuilder` class.  This class has methods of the form: 
```java
public StringBuilder append(...)
``` 
These methods return the `StringBuilder` allowing `append` calls to be chained together, with the `toString()` method ending the chain and building the final String.
```java
String s = new StringBuilder().append("a").append("b").append("c").toString();
```

A key element of a Fluent API is an object returning itself, or returning a related object that can return itself.


### A simple User and UserBuilder

Let's say we have a simple `User` object and we want to build a fluent `UserBuilder`.

Our user has a `name`, `date`, and set of key-value `options`.
```java
public class User {
    String name;
    Date date;
    Map<String, String> opts = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getOption(String key) {
        return opts.get(key);
    }

    public void setOption(String key, String val) {
        this.opts.put(key, val);
    }
}
```

We'll start our `UserBuilder` class.  It will have a `User` object it is building, and will have methods the set the `name`, `date`, and `options` each returning itself.
The `StringBuilder` uses `toString()` as a terminating call to end the chain and return the `String`.  We will need a similar `finalizeUser()` method to return our final `User` object.

```java
public class UserBuilder {
    User user = new User();

    public UserBuilder setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

	public User finalizeUser(){
		return user;
	}
}
```
> In practice, we would likely not hold and modify an actual `User` instance, but some `UserRecord` that would be used to build and populate a full `User` object in the `finalizeUser` method.  In the `Effective Builder Pattern`, instead of a `UserRecord` the `UserBuilder` contains the individual fields used to create the `User`.  We will look more at these approaches later.  For now, I'll say the general _shape_ of the builder, the method signatures, will be the same.

We can start building some `Users`:
```java
User user = new UserBuilder()
        .setDate(date)
        .setName("my name")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```

### Composing Simple Builders

Now we have two builders `StringBuilder` and `UserBuilder`.  
Practicality be damned, I want to use them together to build the `name` of the `User`.  
To do this we can compose the builders, creating and using a `StringBuilder` within the call to `setName`:

```java
User user = new UserBuilder()
        .setDate(date)
        .setName(new StringBuilder()
                .append("my")
                .append(" ")
                .append("name")
                .toString())
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```

Since I am hellbent on using them together, I can build better support into the `UserBuilder` with a lambda approach.  
We'll add this method:
```JAVA
    public UserBuilder buildName(Consumer<StringBuilder> consumer) {
        StringBuilder sb = new StringBuilder();
        consumer.accept(sb);
        user.setName(sb.toString());
        return this;
    }
```

This will do the hard work of creating a `StringBuilder` and calling `toString()` for us.

```java
User user = new UserBuilder()
        .setDate(date)
        .buildName(sb -> sb
                .append("my")
                .append(" ")
                .append("name"))
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```

`new StringBuilder().toString()` is shortened to `sb -> sb`, and as an added bonus we replace the `StringBuilder` instance with some subtype and perform other operations on the built `String` within our `UserBuilder` method, we get dependency injection and inversion of control.

This approach is a cleaner way to compose the builders.  But this is a Fluent API we're building, we want **method chaining**, not composition.

> Bare with me on this silly StringBuilder example a little longer.  We will move on to practical examples soon.

What would it look like if we could chain the two builders together?
```java
User user = new UserBuilder()
        .setDate(date)
        .buildName()
                .append("my")
                .append(" ")
                .append("name")
                .finalizeString()
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
We are replacing the nested `sb -> sb` with the need to call a `finalizeString()` terminating method.  
For this code to work `buildName()` would need to return something like a `StringBuilder`, and `finalizeString()` needs to set the user's `name` and return our `UserBuilder`.  
We will create a new `StringSetter` class.

### The StringSetter

A first sketch of our class would look like this:
```java
public class StringSetter {
    UserBuilder userBuilder;
    StringBuilder sb = new StringBuilder();

    public StringSetter(UserBuilder userBuilder) {
        this.userBuilder = userBuilder;
    }

    public StringSetter append(String str){
        sb.append(str);
        return this;
    }

    public UserBuilder finalizeString(){
        userBuilder.user.setName(sb.toString());
        return userBuilder;
    }
}
```

In our `UserBuilder` we would replace `buildName` with this:
```java
public StringSetter buildName() {
    return new StringSetter(this);
}
```

And now our code works how we want, the Fluent APIs are chained instead of composed:
```java
new UserBuilder()
        .buildName() // returns StringSetter
            .append("my").append(" ").append("name")
            .finalizeString() // sets name and returns UserBuilder
        .setOption("opt1", "v1") // continue building User
```

The compiler and IDE may be happy with our `StringSetter`, but we shouldn't be.  This class can only be attached to a `UserBuilder` and will only work for the `User name` field.  We would have to create a new `StringSetter` class for every string method of every builder type.  
We can do better.

## Generic Chainable Builders

###StringSetter\<R>

We can use a generic type parameter to replace `UserBuilder`, and accept a lambda responsible for setting the final string.
```java
public class StringSetter<R> {
    Consumer<String> setter;
    R returnObject;
    StringBuilder sb = new StringBuilder();

    public StringSetter(Consumer<String> setter, R returnObject) {
        this.setter = setter;
        this.returnObject = returnObject;
    }

    public StringSetter<R> append(String str){
        sb.append(str);
        return this;
    }

    public R finalizeString(){
        setter.accept(sb.toString());
        return returnObject;
    }
}
```
And we update `buildName()` in `UserBuilder`:
```java
public StringSetter<UserBuilder> buildName() {
    return new StringSetter<>(str -> user.setName(str), this);
}
```

The `StringSetter` is now fully generic and re-usable.  If our `User` had a `title` and `email` field we could build those too:
```java
public StringSetter<UserBuilder> buildTitle() {
    return new StringSetter<>(str -> user.setTitle(str), this);
}

public StringSetter<UserBuilder> buildEmail() {
    return new StringSetter<>(str -> user.setEmail(str), this);
}
```
And we can just as easily use it with other builders.
```java
class OtherBuilder {
    public StringSetter<OtherBuilder> buildMessage() {
        return new StringSetter<>(str -> someObj.setMessage(str), this);
    }
}
```

As a quick aside, I'll add a screenshot from `IntelliJ`.  A common complaint about using complex Fluent APIs is getting lost in the chaining and losing track of what type you are using.  Modern IDEs help solve this problem by annotating return types when splitting chained method calls across multiple lines.  In some code examples below I have added comments tp highlight different return types, but in practice my IDE does it for me automatically.  
[IntelliJ screenshot]

The `StringSetter` was fun to build, but not the most useful class in the world.

### MembershipBuilder

Let's add on to our `User` to make it a complex object.  
In our last product meeting it became necessary to add a `Membership` field to our `User`.
```java
public class Membership {
    String groupId;
    String cohortId;
    String membershipId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }
}
```
```java

public class User {
    String name;
    Date date;
    Map<String,String> opts = new HashMap<>();
    Membership primaryMembership;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getOption(String key) {
        return opts.get(key);
    }

    public void setOption(String key, String val) {
        this.opts.put(key, val);
    }

    public Membership getPrimaryMembership() {
        return primaryMembership;
    }

    public void setPrimaryMembership(Membership primaryMembership) {
        this.primaryMembership = primaryMembership;
    }
}
```
Leadership was so impressed with our `StringSetter` work that adding a `MembershipBuilder` to our `UserBuilder` is the *number one priority*.

No problem, copy the pattern in `StringSetter` with methods for the `Membership` fields:
```java
public class MembershipBuilder<R> {
    private Consumer<Membership> setter;
    private R returnObject;
    private Membership membership;

    public MembershipBuilder(Consumer<Membership> setter, R returnObject) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.membership = new Membership();
    }

    public MembershipBuilder<R> groupId(String groupId){
        membership.setGroupId(groupId);
        return this;
    }

    public MembershipBuilder<R> cohortId(String cohortId){
        membership.setCohortId(cohortId);
        return this;
    }

    public MembershipBuilder<R> membershipId(String membershipId){
        membership.setMembershipId(membershipId);
        return this;
    }

    public R finalizeMembership(){
        setter.accept(membership);
        return returnObject;
    }
}
```
Add it to the `UserBuilder`:
```java
public MembershipBuilder<UserBuilder> setPrimaryMembership(){
    return new MembershipBuilder<>(m -> user.setPrimaryMembership(m), this);
}
```
Now we can build a `User` with a `Membership`, a job well done:
```java
User user =  new UserBuilder()
        .setPrimaryMembership() // returns MembershipBuilder
            .groupId("group")
            .cohortId("cohort")
            .membershipId("membership")
            .finalizeMembership() // returns UserBuilder
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
Right as the Chapagne was being opened, the celebrating was interrupted by an `Exception`.  Someone forgot to set a `cohortId` when building a `Membership` and the program crashed.

## Fluent State-Based Builder - MembershipBuild
We want to change the API to ensure all three ids are set.

Instead of one Builder with three methods, we can make three Builders with one method each.  
A `MembershipBuilderGroup` will set the `groupId` and then return a `MembershipBuilderCohort`.  It will set the `cohortId` and return a `MembershipBuilderMember`, which will set the `membershipId`.  The `finalizeMembership()` method can be merged into setting the `membershipId` and is no longer needed.

We'll keep our existing `MembershipBuilder` as shared state, and wrap each method with a non-static inner class.
```java
public class MembershipBuilder<R> {
    private Consumer<Membership> setter;
    private R returnObject;
    private Membership membership;

    private MembershipBuilder(Consumer<Membership> setter, R returnObject) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.membership = new Membership();
    }

    public class MembershipBuilderGroup {
        public MembershipBuilderCohort groupId(String groupId){
            membership.setGroupId(groupId);
            return new MembershipBuilderCohort();
        }
    }

    public class MembershipBuilderCohort {
        public MembershipBuilderPerson cohortId(String cohortId){
            membership.setCohortId(cohortId);
            return new MembershipBuilderPerson();
        }
    }

    public class MembershipBuilderMember {
        public R membershipId(String membershipId){
            membership.setMembershipId(membershipId);
            setter.accept(membership);
            return returnObject;
        }
    }

    public static <T> MembershipBuilder<T>.MembershipBuilderGroup start(Consumer<Membership> setter, T returnObject){
        return new MembershipBuilder<>(setter, returnObject).new MembershipBuilderGroup();
    }
}
```
The constructor is now `private`, `MembershipBuilder` does not have any methods.  We want to an instance of `MembershipBuilder<T>.MembershipBuilderGroup` as our starting point, which is obtained from the `public static` `start` method instead of a constructor.

`UserBuilder` is updated to use the `start` method and return a `MembershipBuilderGroup`.
```java
public MembershipBuilder<UserBuilder>.MembershipBuilderGroup setPrimaryMembership(){
    return MembershipBuilder.start(m -> user.setPrimaryMembership(m), this);
}
```
Using the API is the same, except that we no longer call `finalizeMembership()`, the membership is finalized in the end call to `membershipId`.
```java
User user =  new UserBuilder()
        .setPrimaryMembership() // returns MembershipBuilderGroup
            .groupId("group") // returns MembershipBuilderCohort
            .cohortId("cohort") // returns MembershipBuilderMember
            .membershipId("membership") // returns UserBuilder
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
With this updated `MembershipBuilder`, we  get an error if we try to skip a field.
```java
User user =  new UserBuilder()
        .setPrimaryMembership() // returns MembershipBuilderGroup
            .groupId("group") // returns MembershipBuilderCohort
error ->    .membershipId("membership") // Cannot resolve method 'membershipId' in 'MembershipBuilderCohort'
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```

We can start diagramming our Fluent UserBuilder API a `state-machine`.

```
Start -> UserBuilder -> MembershipBuilderGroup -> MembershipBuilderCohort -> MembershipBuilderMember -> UserBuilder.
```

## Re-using and Extending Builders

Our application continues to grow!  We've added new `Facilitator` and `Roster` classes.

A `Facilitator` is kind of `User` with an additional `facilitatorId` and `facilitatorOptions` fields.

```java
public class Facilitator extends User {
    String facilitatorId;
    Map<String,String> facilitatorOpts = new HashMap<>();
    
    public String getFacilitatorId() {
        return facilitatorId;
    }
    
    public void setFacilitatorId(String facilitatorId) {
        this.facilitatorId = facilitatorId;
    }
    
    public String getFacilitatorOption(String key) {
        return facilitatorOpts.get(key);
    }
    
    public void setFacilitatorOption(String key, String val) {
        facilitatorOpts.put(key, val);
    }
}
```
A `Roster` has a `name`, a `Facilitator`, and a list of `Memberships`.
```java
public class Roster {
    private String name;
    private Facilitator facilitator;
    private List<Membership> memberships = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Facilitator getFacilitator() {
        return facilitator;
    }

    public void setFacilitator(Facilitator facilitator) {
        this.facilitator = facilitator;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void addMembership(Membership membership) {
        this.memberships.add(membership);
    }
}
```

### FacilitatorBuilder - First Attempt

The new `Facilitator` extends `User` and adds new fields.  We'll do the same for a `FacilitatorBuilder`, what could go wrong?
```java
public class FacilitatorBuilder extends UserBuilder {
    public FacilitatorBuilder() {
        super.user = new Facilitator();
    }
    
    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }
    
    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }
    
    @Override
    public Facilitator finalizeUser() {
        return (Facilitator)super.user;
    }
}
```
The `UserBuilder` creates it's `user` instance during construction.  We'll be sneaky and replace it with a `Facilitator`.  The `UserBuilder` can happily set the `User` fields, and we can safely cast to `Facilitator` for our needs (there are ways to avoid casting here, but we'll pinky swear not replace the `user` anywhere else.)  
We do have to Override the `finalizeUser()` method to constrain the return type to `Facilitator` instead of giving us a plain `User`.

Let's try it out:
```java
Facilitator facilitator = new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setFacilitatorOption("f1", "fv1")
        .finalizeUser();
```
The new methods work and it gives us a `Facilitator`!  
Lets set a `User` field.
```java
error -> Facilitator facilitator = new FacilitatorBuilder()
// Incompatible types. Found: 'User', required: 'Facilitator'
        .setFacilitatorId("facilitatorId")
        .setFacilitatorOption("f1", "fv1")
        .setName("name")
        .finalizeUser();
```
This gives an error.  `finalizeUser()` is now returning a `User` instead of returning a `Facilitator` like the previous example.
```java
new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setName("name")
error->     .setFacilitatorOption("f1", "fv1")
// Cannot resolve method 'setFacilitatorOption' in 'UserBuilder'
        .finalizeUser();
```
Now our `Facilitator` methods are gone.  `setName` is returning a `UserBuilder` instead of a `FacilitatorBuilder`, once we call a `UserBuilder` method we are downcast to `UserBuilder`.

It is less than ideal, but we can call our builder methods in order and cast the final `User`.  
It'll have to do for now.

```java
Facilitator facilitator = (Facilitator) new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setFacilitatorOption("f1", "fv1")
        .setName("name")
        .finalizeUser();
```

### RosterBuilder - First Attempt

Let's see what we can do about a `RosterBuilder`.  
Our `UserBuilder`/`FacilitatorBuilder` are not Generic Chainable Builders like the `MembershipBuilder` and `StringSetter`.  We will have to take a Builder Composition approach for setting a `Facilitator` in this first attempt.
```java
public class RosterBuilder {
    private Roster roster = new Roster();

    public RosterBuilder setName(String name) {
        roster.setName(name);
        return this;
    }

    public MembershipBuilder<RosterBuilder>.MembershipBuilderGroup addMembership(){
        return MembershipBuilder.start(m -> roster.addMembership(m), this);
    }

    public RosterBuilder buildFacilitator(Consumer<FacilitatorBuilder> consumer) {
        FacilitatorBuilder userBuilder = new FacilitatorBuilder();
        consumer.accept(userBuilder);
        roster.setFacilitator(userBuilder.finalizeUser());
        return this;
    }
}
```
The `setName` and `addMembership` methods are similar to our `UserBuilder`.  
The `buildFacilitator` method is similar to the earlier `StringBuilder` example:
```java
public UserBuilder buildName(Consumer<StringBuilder> consumer) {
    StringBuilder sb = new StringBuilder();
    consumer.accept(sb);
    user.setName(sb.toString());
    return this;
}
```

Now to build a `Roster`:
```java
Roster roster = new RosterBuilder()
        .setName("roster")
        .buildFacilitator(f -> f
                .setFacilitatorId("facilitatorId")
                .setName("facilitator")
                .setDate(date))
        .addMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .addMembership().groupId("g1").cohortId("c1").membershipId("m2")
        .finalizeRoster();
```
Our roster is built!

When we used the `FacilitatorBuilder` by itself, we resigned ourselves to needing to cast the final `User` to `Facilitator`.  
In our `RosterBuilder` a need to cast never came up.  We hold a direct reference to our `FacilitatorBuilder`.  It doesn't matter if the type at the end of the chain is a `UserBuilder`, the final return is ignored by the `Consumer` and we use our `FacilitatorBuilder` to get a `Facilitator` at the end.

Only the final return type is ignored, the other return types do matter.  We will get an error trying to call a `FacilitorBuilder` method after calling a `UserBuilder` method, as we did before:

```java
Roster roster = new RosterBuilder()
        .setName("roster")
        .buildFacilitator(f -> f
                .setFacilitatorId("facilitatorId")
                .setName("facilitator")
error->         .setFacilitatorOption("f1", "fv1")
// Cannot resolve method 'setFacilitatorOption' in 'UserBuilder'
                .setDate(date))
        .addMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .addMembership().groupId("g1").cohortId("c1").membershipId("m2")
        .finalizeRoster();
```

The first attempt is pretty good.  It does the job.  If it ain't broke, don't fix it. Right?  Is it *ain't broke* though?  I'm looking at an error in the example above.  As was said earlier we're build a Fluent API, we want chaining.

We have two things to fix:  
1. Allow `FacilitatorBuilder`/`UserBuilder` to be chained like `MembershipBuilder` and `StringSetter`.
2. Fix the `FacilitatorBuilder` so it stays a `FacilitatorBuilder` and always returns `Facilitator`.

We've fixed the first problem twice already.  It's a familiar problem we can start with.  We'll come back to look at the second issue, and we'll revert to the current `RosterBuilder` and `FacilitatorBuilder` to solve it separately.  We'll merge the two updates after.


## Generic Chainable Builders - UserBuilder\<R>

We'll make the UserBuilder generic.  We'll add a Type Parameter, a Consumer to set the value, and a return object.
```java
public class UserBuilder<R> {
    protected Consumer<User> setter;
    protected R returnObject;
    protected final User user;

    public UserBuilder(Consumer<User> setter, R returnObject) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.user = new User();
    }

    public UserBuilder<R> setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder<R> setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder<R> setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

    public MembershipBuilder<UserBuilder<R>>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), this);
    }

    public R finalizeUser(){
	    setter.accept(user);
	    return returnObject;
    }
}
```
And we'll update the `FacilitatorBuilder` to match:
```java
public class FacilitatorBuilder<R> extends UserBuilder<R> {
    public FacilitatorBuilder(Consumer<? super Facilitator> setter, R returnObject) {
        super((Consumer<User>)setter, returnObject);
        super.user = new Facilitator();
    }

    public FacilitatorBuilder<R> setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder<R> setFacilitatorOption(String key, String value){
	    ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }
}
```
Our `FacilitatorBuilder` became smaller.  Now that `UserBuilder.finalizeUser` returns `R` instead of `User`, we no longer need to override it to change the return type to `Facilitator`.  We want to leave it returning `R` as defined in `UserBuilder`.

One peculiarity is the use of a lower-bounded wildcard in `Consumer<? super Facilitator>` instead of `Consumer<Facilitator>`.  We did not need a wildcard in the other Builder, what is it doing here?  
We need to make sure `setter` is bounded as `Consumer<Facilitator>`, but we also need it to fit the type bounds `Consumer<User>` to pass it to `UserBuilder` constructor.  A `Consumer<Facilitator>` cannot be cast to a `Consumer<User>`, you will get an `Inconvertible types` error.  User the wildcard replaces the `Inconvertible types` error to an `Unchecked cast` warning.  We know this will only be used with a `Facilitator` at runtime and can ignore the warning.  A different approach would be to create a new `Consumer<User>` to wrap the `Consumer<Facilitator>` and moving the `Unchecked cast` warning.

> Interestingly, we could also use an upper-bounded wildcard `Consumer<? extends Facilitator>` instead of `? super` to fix the `Inconvertible types` error.  Doing this would create a new problem, we could be given a `Consumer` for a subtype of `Facilitator` which call try to call methods a plain `Facilitator` does not have.  It would be the same as trying to call `setFacilitatorId` on a plain `User` object.
>
> It is important to note that the `? super` type lower-bound is not universal.  It is available in `Java`, but not in `C#`.  Generics are implemented differently on the `JVM` and `CLR`.

```java
public FacilitatorBuilder(Consumer<Facilitator> setter, R returnObject) {
    super(user -> setter.accept((Facilitator) user), returnObject);
    super.user = new Facilitator();
}
```
This approach requires invoking two `Consumers` instead of just one, but can be argued to be more understandable without the wildcard.  I find them to be equivalent.


Now it's time to update `RosterBuilder.buildFacilitator` to chain to a `FacilitatorBuilder` instead of using composition.
```java
public FacilitatorBuilder<RosterBuilder> buildFacilitator() {
    return new FacilitatorBuilder<>(user -> roster.setFacilitator(user), this);
}
```
Using the updated `RosterBuilder` works as expected:
```java
Roster roster = new RosterBuilder()
        .setName("roster")
        .buildFacilitator() // return FacilitatorBuilder
            .setFacilitatorId("facilitatorId")
            .setName("facil name")
            .setDate(date)
            .finalizeUser() // returns RosterBuilder
        .addMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .addMembership().groupId("g1").cohortId("c1").membershipId("m2")
        .finalizeRoster();
```

## Chainable and Standalone Builders
However, when we go back to our code using a standalone `UserBuilder` to create a `User` it's a little harder making sense of what to do.
```java
User user = new UserBuilder<>(user -> {???}, ???)
    .setDate(date)
    .setName("my name")
    .setOption("opt1", "v1")
    .setOption("opt2", "v2")
    .finalizeUser();
```
We don't have anything to set, and we don't have anything already created that we want returned.  We want a `User` that hasn't been created yet to be returned.

One approach is to create and hold a reference to be set by the `Consumer` and ignore the returned value.
```java
User[] userRef = new User[1];
new UserBuilder<>(u -> userRef[0] = u, null)
        .setDate(date)
        .setName("my name")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
User user = userRef[0];
```
An ugly hack, but it works.

Another option to make a subtype and override the `finalizeUser` to return the `user` instead of the `returnObject`.  We'll make an anonymous subtype, but could just as easily make a `StandaloneUserBuilder` concrete class.
```java
UserBuilder<User> userBuilder = new UserBuilder<User>(u -> {}, null){
    @Override public User finalizeUser() {
        return super.user;
    }
};
```
We create the `UserBuilder` with an empty `setter` and `returnObject`, then override `finalizeUser()` to return the `user`. The null `returnObject` is never used.
```
User user = userBuilder
        .setDate(date)
        .setName("my name")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
This is an improvement, but we have one more thing to try.

First, let's add a new `UserBuilder` constructor with a `User` parameter.  Instead of having the `UserBuilder` create its own `User`, you can use this to supply a `User` of your choice.
```java
public UserBuilder(Consumer<User> setter, R returnObject, User user) {
    this.setter = setter;
    this.returnObject = returnObject;
    this.user = user;
}
```
We'll make the same change in `FacilitatorBuilder`
```java
public FacilitatorBuilder(Consumer<Facilitator> setter, R returnObject, Facilitator facilitator) {
    super(user -> setter.accept((Facilitator) user), returnObject, facilitator);
}
```
As an added bonus, our `FacilitatorBuilder` no longer needs to replace the `UserBuilder` `user`.  In fact, we can now declare `user` to be `final` and rest easy knowing it cannot be changed unexpectedly for a `FacilitorBuilder`.

With this new constructor, we can create a `User` and pass the same object as both the `returnObject` and the `user`.  This object is modified during building, and then returned as the `returnObject` at the end.
```java
User newUser = new User();
User user = new UserBuilder<>(u -> {}, newUser, newUser)
        .setDate(date)
        .setName("my name")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
Much better than needing to override a method.  
We can add a static convenience method to `UserBuilder` if desired.
```java
public static UserBuilder<User> standalone(){
    User user = new User();
    return new UserBuilder<>(u -> {}, user, user);
}
```
And now we no longer need to create a User or other special setup to use it as a standalone builder.
```java
User user = UserBuilder.standalone()
        .setDate(date)
        .setName("my name")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
We also want to use the `FacilitatorBuilder` as a standalone builder, so we will need to add a similar method there.
```java
public static FacilitatorBuilder<Facilitator> standalone(){ // Error!
    Facilitator facilitator = new Facilitator();
    return new FacilitatorBuilder<>(null, facilitator, facilitator);
}
```
This gives us an error: `'standalone()' in 'FacilitatorBuilder' clashes with 'standalone()' in 'UserBuilder'; attempting to use incompatible return type`.  And now we know that static methods can clash, not just members methods.    
A `FacilitatorBuilder<Facilitator>` cannot convert to a `UserBuilder<User>`.  It can, however, convert to a `UserBuilder<Facilitator>`.  
`UserBuilder<Facilitator>` also does not convert to `UserBuilder<User>` they do not match.  The type parameter are defined to be exact.  
A `UserBuilder<Facilitator>` would match against `UserBuilder<? extends User>`, and if those match then our `FacilitatorBuilder<Facilitator>` should also match.

We'll update `UserBuild` to add the wildcard:

```java
public static UserBuilder<? extends User> standalone(){
    User user = new User();
    return new UserBuilder<>(u -> {}, user, user);
}
```
And just like that, the methods no longer clash and the `FacilitatorBuilder` error goes away.  Our `FacilitatorBuilder` can be used as a standalone builder again.
```java
Facilitator facilitator = FacilitatorBuilder.standalone()
        .setFacilitatorId("facilitatorId")
        .setFacilitatorOption("f1", "fv1")
        .setName("name")
        .setDate(date)
        .finalizeUser();
```

We fixed our first issue, `FacilitatorBuilder` is now chained to `RosterBuilder` instead of composed.  We can also continue to use it as a standalone builder.
1. ~~Allow `FacilitatorBuilder`/`UserBuilder` to be chained like `MembershipBuilder` and `StringSetter`.~~

## Fluent APIs and Inheritance
2. Fix the `FacilitatorBuilder` so it stays a `FacilitatorBuilder` and always returns `Facilitator`.

Now we'll revert to the earlier non-generic `UserBuilder` and `FacilitatorBuilder` and look at the second issue.  We're reverting the code to look at the problem in isolation with simpler non-generic builders, we do not want to confuse the two different issues and their different solutions with each other.

The `UserBuilder`:
```java
public class UserBuilder {
    protected User user = new User();

    public UserBuilder setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

    public MembershipBuilder<UserBuilder> setPrimaryMembership(){
        return new MembershipBuilder<>(m -> user.setPrimaryMembership(m), this);
    }

    public User finalizeUser(){
        return user;
    }
}
```
The `FacilitatorBuilder`:
```java
public class FacilitatorBuilder extends UserBuilder {
    public FacilitatorBuilder() {
        super.user = new Facilitator();
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }

    @Override
    public Facilitator finalizeUser() {
        return (Facilitator)super.user;
    }
}
```
Our problem code returns a `User` instead of a `Facilitator` after calling `UserBuilder.setName`:
```java
new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setFacilitatorOption("f1", "fv1")
        .setName("name")
        .finalizeUser();
```
A big clue is right there at the end of our `FacilitatorBuilder`:
```java
@Override
public Facilitator finalizeUser() {
    return (Facilitator)super.user;
}
```
We had to override `finalizeUser` to return the correct type, otherwise it would return a `User`.  
We did not override `setName`, so why would we expect it to return anything other than a `UserBuilder`.
```java
@Override
public FacilitatorBuilder setName(String name) {
    super.setName(name);
    return this;
}
```
Now that we tell it to return what we want, the code works and gives us a `Facilitator`.  We are also able to call `Facilitator` methods after calling `setName`.
```java
Facilitator facilitator = new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setName("name")
        .setFacilitatorOption("f1", "fv1")
        .finalizeUser();
```
We've fixed one method, but we'll need to override all of the `UserBuilder` methods to update the return types.  And if we ever want to extend `FacilitatorBuilder` we have to do it all over again.  
It's boilerplate work, we could probably automate it some way.  And ignore all the clutter...  
Wasn't the whole point of using inheritance for us to avoid redefining things over and over again?  
We didn't settle for writing a new `StringSetter` for every field of every builder.  We found a way to make it generic.

Let's do the same thing here, we'll parameterize the return type, easy-peasy.
```java
public class UserBuilder<UB> {
    protected User user = new User();

    public UB setName(String name) {
        user.setName(name);
        return (UB)this;
    }
    
    ...
```
And our `FacilitatorBuilder` can set that parameter to itself:
```java
public class FacilitatorBuilder extends UserBuilder<FacilitatorBuilder> {
```
We can remove the overridden `setName` in `FacilitatorBuilder` and our code still works, and works for all `UserBuilder` methods.

The big problem is the `UB` is an unconstrained parameter. What is `UserBuilder<File>` supposed to mean?
The compiler has no problem with a `UserBuilder<File>`, but at runtime this is going to fail `return (UB)this;`, we know we can't cast a `UserBuilder` to a `File`.  Our `FacilitatorBuilder` works by have the good fortune of the types lining up.  The `UserBuilder` is no longer type-safe.

`UB` needs to be bounded to disallow any type that would fail to cast.  From the perspective of the `UserBuilder` it needs to be at least a `UserBuilder` or some subtype.

`UB extends UserBuilder`, and since our `UserBuilder` now has a type parameter it needs to be included.
`UB extends UserBuilder<?>` What to put there?  It seems to be a `UserBuilder` sized hole, and `UB` looks like a `UserBuilder` sized peg...

`UserBuilder<UB extends UserBuilder<UB>>` hmmm, strange type...  looks a little recursive...

Compiler seems happy with it.  Code still works.  The compiler won't allow `UserBuilder<File>` anymore, and `UserBuilder<FacilitatorBuilder>` is still in bounds.  Seems we're on to something with this.

## F-Bounded Polymorphic Builder

We have stumbled onto [F-Bounded Polymorphism](https://www.cs.utexas.edu/~wcook/papers/FBound89/CookFBound89.pdf), also called a [Curiously Recurring Template Pattern (CRTP)](https://en.wikipedia.org/wiki/Curiously_recurring_template_pattern) in C++.  
In the Java, the Enum base class is defined as `Enum<E extends Enum<E>>`

Here is F-Bounded `UserBuilder`:
```java
public class UserBuilder<UB extends UserBuilder<UB>> {
    protected final User user;

    public UserBuilder() {
        this(new User());
    }

    public UserBuilder(User user) {
        this.user = user;
    }

    public UB setName(String name) {
        user.setName(name);
        return (UB)this;
    }

    public UB setDate(Date date) {
        user.setDate(date);
        return (UB)this;
    }

    public UB setOption(String key, String value){
        user.setOption(key, value);
        return (UB)this;
    }

    public MembershipBuilder<UB>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), (UB)this);
    }

    public User finalizeUser(){
        return user;
    }
}
```

The only change to the `FacilitatorBuilder` is the one we made above, filling in the type parameter with itself.
```java
public class FacilitatorBuilder extends UserBuilder<FacilitatorBuilder> {
    public FacilitatorBuilder() {
        super.user = new Facilitator();
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }

    @Override
    public Facilitator finalizeUser() {
        return (Facilitator)super.user;
    }
}
```
We can build a `Facilitator`:
```java
Facilitator facilitator = new FacilitatorBuilder()
        .setFacilitatorId("facilitatorId")
        .setName("name")
        .finalizeUser();
```
How do we build a plain `User`?  What do set for the `UserBuilder` type parameter?  We can't define a `UserBuilder` within terms of itself.
```java
new UserBuilder<UserBuilder<?>>()
```

If the generic parameters are left out, the `UserBuilder` will work at first.
```java
User user = new UserBuilder()
        .setDate(date)
        .setName("my name")
        .finalizeUser();
```
Without the generic parameter, `UB` is treated as `UserBuilder<Object>`.  Calling `setName` or `setDate` will return the same `UserBuilder<Object>` and continue to work.  
But, calling `setPrimaryMembership` causes an error.
```java
User user = new UserBuilder()
        .setDate(date)
        .setName("my name") // returns UserBuilder<Object>
        .setPrimaryMembership() // returns MembershipBuilder<Object>
            .groupId("group")
            .cohortId("cohort")
            .membershipId("membership") // returns Object
error-> .setOption("opt1", "v1")
// Cannot resolve method 'setOption' in 'Object'
        .setOption("opt2", "v2")
        .finalizeUser();
```
It turns out we can use Java's `<>` diamond operator to infer a type.  We cannot use the `<>` operator on the left-hand side. There we need a wildcard `<?>` which will be bounded by `UserBuilder`:
```java
UserBuilder<?> userBuilder = new UserBuilder<>();
userBuilder
        .setDate(date)  // returns 'capture of ?'
        .setName("my name")
        .setPrimaryMembership() // returns MembershipBuilder<capture of ?>
            .groupId("group")
            .cohortId("cohort")
            .membershipId("membership") // returns 'capture of ?'
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```

Another solution if you want to avoid wildcards and inference is to create a new class.  There's no class body required, we only need the type definition:
```java
public class PlainUserBuilder extends UserBuilder<PlainUserBuilder> { }
```

```java
PlainUserBuilder userBuilder = new PlainUserBuilder();
user = userBuilder
        .setDate(date)
        .setName("my name")
        .setPrimaryMembership()
            .groupId("group")
            .cohortId("cohort")
            .membershipId("membership")
        .setOption("opt1", "v1")
        .setOption("opt2", "v2")
        .finalizeUser();
```
Our `F-Bounded Polymorphic UserBuilder` is looking good, and `FacilitatorBuilder` extends it without any extra work.  
It looks like we've solved our second issue.
2. ~~Fix the `FacilitatorBuilder` so it stays a `FacilitatorBuilder` and always returns `Facilitator`.~~

Before we start merging our fixes together, there's one more thing I want to look at with this...

## Beyond F-Bounded Polymorphism
Some new application requirements came.  Turns out there is a special group of 'Fancy' `Facilitators`.  There is no `FancyFacilitator` class.  They are regular `Facilitator` objects and don't have any extra fields.  When they're built there's a little something extra done to make them, ya'know... fancy.

Our requirements are simple.  A 'Fancy' `Facilitator` is a `Facilitator` with the `facilitatorOption` of `fancy="yes"`.  

### More Inheritance
We need to create a `FancyFacilitatorBuilder` sets `fancy="yes` in `finalizeUser()`.
```java
class FancyFacilitatorBuilder extends FacilitatorBuilder {
    @Override
    public Facilitator finalizeUser() {
        this.setFacilitatorOption("fancy", "yes");
        return super.finalizeUser();
    }
}
```
Simple. One override, set the option, and call it a day.

```java
Facilitator facilitator = new FancyFacilitatorBuilder()
        .setName("name")
        .setPrimaryMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .setFacilitatorId("fId")
        .finalizeUser();
assert facilitator.getFacilitatorOption("fancy").equals("yes");
```
The next day a message comes in from another developer:
> Trouble with FancyFacilitatorBuilder!  
> Desperately need to create a `UserBuilder<FancyFacilitatorBuilder>`!  
> `UserBuilder<FacilitatorBuilder>` Okay!  
> Advise immediately!!!

Short on details, but we're ready to jump in.
```java
UserBuilder<FacilitatorBuilder> facilitatorBuilder;
UserBuilder<FancyFacilitatorBuilder> extendedFacilitatorBuilder;
            ^^^Error
// Type parameter 'FancyFacilitatorBuilder' is not within its bound; should extend 'UserBuilder<FancyFacilitatorBuilder>'
```
Right away we get the error.  The compiler does not like it.  
Our types:
* `UserBuilder<T extends UserBuilder<T>>`
* `FacilitatorBuilder extends UserBuilder<FacilitatorBuilder>`
* `FancyFacilitatorBuilder extends FacilitatorBuilder`

`FancyFacilitatorBuilder` -> `FacilitatorBuilder` -> `UserBuilder<FacilitatorBuilder>`

`FancyFacilitatorBuilder` appears to be a `UserBuilder<FacilitatorBuilder>` instead of a `UserBuilder<FancyFacilitatorBuilder>`
We are telling the compiler we want:
```
UserBuilder<FancyFacilitatorBuilder extends UserBuilder<FacilitatorBuilder>>
```
and the compiler is saying that

```
> T extends UserBuilder<X> != T extends UserBuilder<T>
> X != T
```
### Upper and Lower Bounds
Earlier when making the generic chainable version of `UserBuilder`, after adding a second `U extends User` type parameter we had two errors come up the we fixed by changing the type bounds:

1. We were unable to cast a `Consumer<Facilitator>` to a `Consumer<User>` in the `FacilitatorBuildor` constructor.

2. method signature clash:
```
UserBuilder:
public static UserBuilder<User> standalone()
```

```
FacilitatorBuilder:
public static FacilitatorBuilder<Facilitator> stadalone()
```

These were fixed by setting lower (`? super`) and upper (`? extends`) type bounds.
```java
public FacilitatorBuilder(Consumer<? super Facilitator> setter, R returnObject) {
public static UserBuilder<? extends User> standalone(){
```

#### Upper and Lower Bounds: Which to use?

We want to make the type `FancyFacilitatorBuilder extends UserBuilder<FacilitatorBuilder>` valid.  
To do that we need to adjust `T extends UserBuilder<T>` to match.

If `FancyFacilitatorBuilder extends FacilitatorBuilder` then `FacilitatorBuilder super FancyFacilitatorBuilder`

Plugging those in as a `UserBuilder` parameter for `FancyFacilitatorBuilder` gives us:
```java
FancyFacilitatorBuilder extends UserBuilder<FancyFacilitatorBuilder extends FacilitatorBuilder>
FancyFacilitatorBuilder extends UserBuilder<FacilitatorBuilder super FancyFacilitatorBuilder>
```
`FancyFacilitatorBuilder` is the type we're working with, so replace it with `T`  
```java
T extends UserBuilder<T extends FacilitatorBuilder>
T extends UserBuilder<FacilitatorBuilder super T>
```
`FacilitatorBuilder` is the type we're trying to match in the parameter, so replace it with `?`:
```java
T extends UserBuilder<T extends ?>
T extends UserBuilder<? super T>
```
The second one is the valid syntax, we are looking for the `?` to be on the left-hand side.  We want `? super`:
```java
public class UserBuilder<UB extends UserBuilder<? super UB>> {
```
No other changes made, our error is gone, and we are free to make`UserBuilder<FancyFacilitatorBuilder>`.
```java
UserBuilder<FancyFacilitatorBuilder> wrappedFancyBuilder = new UserBuilder<FancyFacilitatorBuilder>();
Facilitator facilitator = wrappedFancyBuilder
        .setName("name")
        .setFacilitatorId("asdf")
        .setPrimaryMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .setFacilitatorId("fId")
        .finalizeUser();
```
We've relaxed the F-Bounded constraint to allow any type between `UserBuilder` and `UB` instead of only allowing `UB`.  This improves the support for extending F-Bounded types.
This gives us the freedom to wrap and unwrap descendant subtypes.  This can allow for special wrapper types that temporarily alter the builder behavior.  
This change goes beyond inheritance, as we saw with our `Consumer<? super Facilitator>`, it can enable limited casting of the inner Type parameter along with the normal casting of the outer Type.  We could cast (with an unchecked cast warning) our `Consumer<? super Facilitator>` to `Consumer<User>`. 

There is a big caveat with extending an F-Bound type, one that is not fixed by the `? super` lower bound:

```java
Facilitator facilitator = new FancyFacilitatorBuilder() // returns FancyFacilitatorBuilder
        .setName("name") // returns FacilitatorBuilder
        .setPrimaryMembership().groupId("g1").cohortId("c1").membershipId("m1") // returns FacilitatorBuilder
        .setFacilitatorId("fId") // returns FacilitatorBuilder
        .finalizeUser();
```
After the first call to `setName` (or any other builder method), a `FacilitatorBuilder` is returned instead of `FancyFacilitatorBuilder`.

While `UserBuilder` now allows `FancyFacilitatorBuilder` as a parameter.  It is important to remind ourselves that we needed the lower bound because `FancyFacilitatorBuilder` was actually extending `UserBuilder<FacilitatorBuilder>` to start with.  We've only changed what `UserBuilder` allows as a parameter, but we did not change any parameters of any subtypes.  The `<T>` for `FancyFacilitatorBuilder` is still `<FacilitatorBuilder>`, and all the methods still return `T`.

For our purposes this is not a problem.  The `FancyFacilitatorBuilder` **does not add methods** and **does not change method signatures** of `FacilitatorBuilder`, it is only altering the behavior of existing methods.  It does not matter which Type the compiler thinks it is  since they have the same methods, and the run-time behavior will be that of the `FancyFacilitatorBuilder`.

If we were adding or changing the methods, we only see those on the first method call.  Once we call a pre-existing method, we change to super class.

If `FancyFacilitatorBuilder` needed an extra method, we would have to propagate the F-Bounded constrain through `FacilitatorBuilder` (with or without the `? super`):
```java
public class FacilitatorBuilder<FB extends FacilitatorBuilder<FB>> extends UserBuilder<FB> {
public class FacilitatorBuilder<FB extends FacilitatorBuilder<? super FB>> extends UserBuilder<FB> {
```
```java
public class FancyFacilitatorBuilder extends FacilitatorBuilder<FancyFacilitatorBuilder> {
    public FancyFacilitatorBuilder fancyAction() {...}
}
```
And now `FancyFacilitatorBuilder` is a proper `UserBuilder<FancyFacilitatorBuilder>` and will retain any additional methods.
```java
Facilitator facilitator = new FancyFacilitatorBuilder()
        .setName("name")
        .fancyAction() // new 'fancy' method is still available
        .setPrimaryMembership().groupId("g1").cohortId("c1").membershipId("m1") 
        .setFacilitatorId("fId") 
        .finalizeUser();
```

However, our code building a `UserBuilder<FacilitatorBuilder>` is now broken.  
We added a type parameter to `FacilitatorBuilder<FB>` which determines the `UserBuilder` type.  Without a type parameter on `FacilitatorBuilder` it loses any generic information, `<FB>` becomes `<Object>` and it is seen as a `UserBuilder<Object>` which is no longer in bounds.

We need to fill in `UserBuilder<FacilitatorBuilder<?>`.  
Since our `FancyFacilitatorBuilder` is now a proper `UserBuilder<FancyFacilitatorBuilder>`, we no longer have a concrete class that matches `UserBuilder<FacilitatorBuilder>`.

In order to make a matching type, we would have to define it in terms of `FacilitatorBuilder<FB>`, but we already know we do not have a type to fit into `<FB>` that is what we're trying to make, if we had one, we would have used it.
The closest you can get to defining `UserBuilder<FacilitatorBuilder>` is in defining a generic method type `T` bounded by `FacilitatorBuilder`:
```java
public <T extends FacilitatorBuilder<T>> void test(T builder){
    UserBuilder<T> facilitatorBuilder = new FacilitatorBuilder();
}
```
`T` evaluates to its bounds of `FacilitatorBuilder<>`, and `UserBuilder<T>` is the closest you can get to `UserBuilder<FacilitatorBuilder<>>`

You can avoid needing the `? super` by propagating the F-Bounded constraints, but the concrete classes allowed by `? super` can be much easier to work with than their F-Bounded counterparts.

I will note that the `? super` lower bound operator is specific to Java's implementation of generics.  `C#`/`CLR` does not have an equivalent operator and `C#` is limited to the normal strict F-Bounded Polymorphism.

That said, we can re-write `Type<T extends Type<? super T>>` without the `?` wildcard as a normal F-Bounded type by replacing the `?` wildcard with another type:
```java
// Generic Method Type Parameter
<S extends Type<S>, T extends S>
```
`S extends Type<S>` replaces the `?` wildcard as the type directly extending `Type`, and `T extends S` is expanded as `T extends S extends Type<S>`.  Knowing this is a recursive parameter we can rewrite it as `T extends Type<S extends Type<S>>` which is the same as `T extends Type<? super T>`.  In this formulation we get both `T` the type itself and `S` the parent type by naming them separately.

Wrapping up on the F-Bounded `UserBuilder`, the only difference in our latest version the addition of `? super`.:
```java
public class UserBuilder<UB extends UserBuilder<? super UB>> {
    protected User user;

    public UserBuilder() {
        this.user = new User();
    }

    public UB setName(String name) {
        user.setName(name);
        return (UB)this;
    }

    public UB setDate(Date date) {
        user.setDate(date);
        return (UB)this;
    }

    public UB setOption(String key, String value){
        user.setOption(key, value);
        return (UB)this;
    }

    public MembershipBuilder<UB>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), (UB)this);
    }

    public User finalizeUser(){
        return user;
    }
}
```
The `FacilitatorBuilder` is unchanged:
```java
public class FacilitatorBuilder extends UserBuilder<FacilitatorBuilder> {
    public FacilitatorBuilder() {
        super.user = new Facilitator();
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }

    @Override
    public Facilitator finalizeUser() {
        return (Facilitator)super.user;
    }
}
```


## Merging the Chainable and F-Bounded Builders
We solved the two problems we had after making the `RosterBuilder`
1. Allow `FacilitatorBuilder`/`UserBuilder` to be chained like `MembershipBuilder` and `StringSetter`.
2. Fix the `FacilitatorBuilder` so it stays a `FacilitatorBuilder` and always returns `Facilitator`.

We just saw the F-Bounded version which we'll merge with this Chainable version:
```java
public class UserBuilder<R> {
    protected Consumer<User> setter;
    protected R returnObject;
    protected User user;

    public UserBuilder(Consumer<User> setter, R returnObject, User user) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.user = user;
    }

    public UserBuilder(Consumer<User> setter, R returnObject) {
        this(setter, returnObject, new User());
    }

    public UserBuilder<R> setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder<R> setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder<R> setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

    public MembershipBuilder<UserBuilder<R>>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), this);
    }

    public R finalizeUser(){
        setter.accept(user);
        return returnObject;
    }

    public static UserBuilder<? extends User> standalone(){
        User user = new User();
        return new UserBuilder<>(u -> {}, user, user);
    }
}
```

Putting them together we get:
```java
public class UserBuilder<R, UB extends UserBuilder<R, ? super UB>> {
    protected Consumer<User> setter;
    protected R returnObject;
    protected User user;

    public UserBuilder(Consumer<User> setter, R returnObject, User user) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.user = user;
    }

    public UserBuilder(Consumer<User> setter, R returnObject) {
        this(setter, returnObject, new User());
    }

    public UB setName(String name) {
        user.setName(name);
        return (UB)this;
    }

    public UB setDate(Date date) {
        user.setDate(date);
        return (UB)this;
    }

    public UB setOption(String key, String value){
        user.setOption(key, value);
        return (UB)this;
    }

    public MembershipBuilder<UB>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), (UB)this);
    }

    public R finalizeUser(){
        setter.accept(user);
        return returnObject;
    }

    public static <T extends UserBuilder<? extends User, ? super T>> T standalone(){
        User user = new User();
        return (T)new UserBuilder<>(u -> {}, user, user);
    }
}
```

The merged `FacilitatorBuilder`:
```java
public class FacilitatorBuilder<R> extends UserBuilder<R, FacilitatorBuilder<R>> {
    public FacilitatorBuilder(Consumer<Facilitator> setter, R returnObject) {
        this(setter, returnObject, new Facilitator());
    }

    public FacilitatorBuilder(Consumer<? super Facilitator> setter, R returnObject, Facilitator facilitator) {
        super((Consumer<User>) setter, returnObject, facilitator);
    }

    public FacilitatorBuilder<R> setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder<R> setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }

    public static FacilitatorBuilder<Facilitator> standalone(){
        Facilitator facilitator = new Facilitator();
        return new FacilitatorBuilder<>(u -> {}, facilitator, facilitator);
    }
}
```
There were no issues merging the two to produce a Chainable F-(lower)-Bounded Polymorphic `UserBuilder` and `FacilitatorBuilder`.

## Configurable Builders
The one thing that still stands out is all of the `(Facilitator)` casting in `FacilitatorBuilder`.  It seems like it shouldn't be needed, the `FacilitatorBuilder` should __*know*__ it is building a `Facilitator`, and the `UserBuilder` should __*know*__ it is building a `User`.

Let's jump back, one final time, to our simple `UserBuilder`, and look at adding a `User` type parameter to our `UserBuilder`.  
We'll look at this change in isolation before merging it.

Here are the simple builders again:
```java
public class UserBuilder {
    protected User user = new User();

    public UserBuilder setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

    public MembershipBuilder<UserBuilder> setPrimaryMembership(){
        return new MembershipBuilder<>(m -> user.setPrimaryMembership(m), this);
    }

    public User finalizeUser(){
        return user;
    }
}
```
```java
public class FacilitatorBuilder extends UserBuilder {
    public FacilitatorBuilder() {
        super.user = new Facilitator();
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        ((Facilitator)user).setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        ((Facilitator)user).setFacilitatorOption(key, value);
        return this;
    }

    @Override
    public Facilitator finalizeUser() {
        return (Facilitator)super.user;
    }
}
```

### Configurable UserBuilder\<U>
Add a type parameter `<U extends User>` give us this:
```java
public class UserBuilder<U extends User> {
    protected final U user;

    public UserBuilder(U user) {
        this.user = user;
    }

    public UserBuilder<U> setName(String name) {
        user.setName(name);
        return this;
    }

    public UserBuilder<U> setDate(Date date) {
        user.setDate(date);
        return this;
    }

    public UserBuilder<U> setOption(String key, String value){
        user.setOption(key, value);
        return this;
    }

    public MembershipBuilder<UserBuilder<U>> setPrimaryMembership(){
        return new MembershipBuilder<>(m -> user.setPrimaryMembership(m), this);
    }

    public U finalizeUser(){
        return user;
    }

    public static UserBuilder<? extends User> standalone(){
        User user = new User();
        return new UserBuilder<>(user);
    }
}
```
This looks nearly identical to the Generic Chainable `UserBuilder`:
The constructor now takes a `U user` instead of creating the instance itself.
It has the same `public static UserBuilder<? extends User> standalone()` in place of a default constructor, acting as  `User` factory to create a base instance.  
Without looking at the `FacilitatorBuilder`, I can guess that `public U finalizeUser()` will not need to be overridden in this version either.

The updated `FacilitatorBuilder`:
```java
public class FacilitatorBuilder extends UserBuilder<Facilitator> {
    public FacilitatorBuilder() {
        this(new Facilitator());
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        user.setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        user.setFacilitatorOption(key, value);
        return this;
    }
}
```
Wow, that is much cleaner.  No casting, no override.

We can choose to also parameterize `FacilitatorBuilder` if we are expecting subtypes of `Facilitator`:
```java
public class FacilitatorBuilder<U extends Facilitator> extends UserBuilder<U> {
    public FacilitatorBuilder(U facilitator) {
        super(facilitator);
    }

    public FacilitatorBuilder setFacilitatorId(String facilitatorId) {
        user.setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder setFacilitatorOption(String key, String value){
        user.setFacilitatorOption(key, value);
        return this;
    }

    public static FacilitatorBuilder<? extends Facilitator> standalone(){
        Facilitator user = new Facilitator();
        return new FacilitatorBuilder<>(user);
    }
}
```
Just like the `UserBuilder`, the constructor now takes a `U facilitator` object, and we have the static `standalone()` method in place of the default constructor.

We don't have any plans to subtype `Facilitator`, but we'll stick with

## The Configurable Chainable Polymorphic Builder
Let's merge it all together and take a look.
```java
public class UserBuilder<U extends User, R, UB extends UserBuilder<U, R, ? super UB>> {
    protected Consumer<U> setter;
    protected R returnObject;
    protected U user;

    public UserBuilder(Consumer<U> setter, R returnObject, U user) {
        this.setter = setter;
        this.returnObject = returnObject;
        this.user = user;
    }

    public UB setName(String name) {
        user.setName(name);
        return (UB)this;
    }

    public UB setDate(Date date) {
        user.setDate(date);
        return (UB)this;
    }

    public UB setOption(String key, String value){
        user.setOption(key, value);
        return (UB)this;
    }

    public MembershipBuilder<UB>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> user.setPrimaryMembership(m), (UB)this);
    }

    public R finalizeUser(){
        setter.accept(user);
        return returnObject;
    }

    public static <T extends UserBuilder<T,User,User>> T standalone(){
        User user = new User();
        return (T)new UserBuilder<>(u -> {}, user, user);
    }
}
```
The new `U` type parameter fits in cleanly.  It doesn't change any of our methods, only parameterizes `User` to `U`.

The `standalone()` method is somewhat simplified.  We no longer need the type bounds on each type parameter.  We'll see that that `FacilitatorBuilder`'s `standalone()` method no longer clashes with `User` parameterized to `U`.  
That's good, because it was getting long with them all in.  We won't have to suffer with:
```java
public static <T extends UserBuilder<? extends User, ? extends User, ? super T>> T standalone(){
```
Our combined `UserBuilder` doesn't look much different, if anything a little cleaner.

Now for the `FacilitatorBuilder`:
```java
public class FacilitatorBuilder<U extends Facilitator, R> extends UserBuilder<U, R, FacilitatorBuilder<U,R>> {
    public FacilitatorBuilder(Consumer<U> setter, R returnObject, U facilitator) {
        super(setter, returnObject, facilitator);
    }

    public FacilitatorBuilder<U,R> setFacilitatorId(String facilitatorId) {
        user.setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder<U,R> setFacilitatorOption(String key, String value){
        user.setFacilitatorOption(key, value);
        return this;
    }

    public static FacilitatorBuilder<Facilitator,Facilitator> standalone(){
        Facilitator facilitator = new Facilitator();
        return new FacilitatorBuilder<>(u -> {}, facilitator, facilitator);
    }
}
```
And if we did not want to parameterize the `Facilitator` type, it would be:
```java
public class FacilitatorBuilder<R> extends UserBuilder<Facilitator, R, FacilitatorBuilder<R>> {
    public FacilitatorBuilder(Consumer<Facilitator> setter, R returnObject) {
        super(setter, returnObject, new Facilitator());
    }

    public FacilitatorBuilder<R> setFacilitatorId(String facilitatorId) {
        user.setFacilitatorId(facilitatorId);
        return this;
    }

    public FacilitatorBuilder<R> setFacilitatorOption(String key, String value){
        user.setFacilitatorOption(key, value);
        return this;
    }
}
```
No tedious boilerplate.  All the work is done in the type system.  Just extend the `UserBuilder` and add your new methods.

Finally, the `RosterBuilder`.  The only change here is updating the `FacilitatorBuilder` type parameters.
```java
public class RosterBuilder {
    private Roster roster = new Roster();

    public RosterBuilder setName(String name) {
        roster.setName(name);
        return this;
    }

    public FacilitatorBuilder<Facilitator,RosterBuilder> buildFacilitator() {
        return new FacilitatorBuilder<>(user -> roster.setFacilitator(user), this, new Facilitator());
    }

    public MembershipBuilder<RosterBuilder>.MembershipBuilderGroup addMembership(){
        return MembershipBuilder.start(m -> roster.addMembership(m), this);
    }

    public Roster finalizeRoster(){
        return roster;
    }
}
```

Now, for what we've all ben waiting for.  Let's make a `Roster`!
```java
Roster roster = new RosterBuilder()
        .setName("roster") // returns RosterBuilder
        .buildFacilitator() // returns FacilitatorBuilder
                .setName("facil name")
                .setDate(date)
                .setPrimaryMembership() // returns MembershipBuilder
                        .groupId("g12").cohortId("c31").membershipId("m11") // returns FacilitatorBuilder
                .setFacilitatorId("facilitatorId")
                .finalizeUser() // returns RosterBuilder
        .addMembership().groupId("g1").cohortId("c1").membershipId("m1")
        .addMembership().groupId("g1").cohortId("c1").membershipId("m2")
        .finalizeRoster();s
```

As displayed by `IntelliJ` with automatic type annotations:

[img todo](screenshot)

Next we'll look at adding one more type parameter specific to Builder types, separate types for a Record to be modified by the Builder, and a Value to be constructed from the Record. 

## A Fourth Parameter - Adding a UserRecord

So far we've been working with a `UserBuilder` which takes a constructed `User` object and configures it.  Many Builders are built this way, but often Builders are made to collect a set of parameters needed for constructing an object.  This is the design of the `Effective Builder` which we'll look at in some detail.  The outcome is the same, the Builder provides a fully instantiated object, but the implementations necessarily differ.

A common pattern is to define the necessary fields within the Builder itself and set those.  Then use those fields to create the instance.  I have found it is useful to take it one step further and define a serializable `UserRecord` to hold the builder fields, and the builder has a `UserRecord` instead of having the fields directly.  I have found that a serializable record is useful in many other contexts like messaging, import/export, third-party integration, reporting, logging, and testing.

The example `UserBuilder` has a `User` instance, sets its fields, and then returns that `User`.

We would expect a real `UserBuilder` to instead:
1. have a `UserRecord` instance
2. set the record fields
3. create a full `User` instance from the `UserRecord`
4. do something with the `User`

Since out `User` has `Memberships`, we will make the same changes for that class by adding a `MembershipRecord`.

Our `MembershipRecord` and `UserRecord` will be very simple:
```java
public class MembershipRecord {
    public String groupId;
    public String cohortId;
    public String membershipId;
}
```
```java
public class UserRecord {
    public String name;
    public Date date;
    public Map<String,String> opts = new HashMap<>();
    public MembershipRecord primaryMembership;
    public MembershipRecord secondaryMembership;
    public List<MembershipRecord> memberships = new ArrayList<>();
}
```
We'll update the `Membership` and `User` classes to construct from a record.  We'll also hide the field setters to better represent a real-world object where some fields cannot be changed after the object in created.
```java
public class Membership {
    private String groupId;
    private String cohortId;
    private String membershipId;

    public Membership(MembershipRecord record){
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
}
```
```java
public class User {
    private String name;
    private Date date;
    private Map<String,String> opts;
    private Membership primaryMembership;
    private Membership secondaryMembership;
    private List<Membership> memberships;

    public User(UserRecord record){
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
}
```

We expect a `UserBuilder` to build a `User` from a `UserRecord`.  We will call methods on the `UserBuilder` to set fields on the `UserRecord`.  When done we will construct a `User` to be consumed.

Rather than take a `User` instance in the constructor it will take a `UserRecord`.  The `Consumer<User> setter` will continue to consume a `User`.  This is separating the input and output types, they are no longer bound to be the same.  In order to convert the `UserRecord` to a `User` we will use a `Function<UserRecord,User> factory`.  For this example the `factory` function will be `record -> new User(record)`, in practice this may be much more complex.  Instead of having a `Function` constructor parameter, a `User build(UserRecord record)` method could be added and overridden when a change is neede.

Instead of hardcoding the `UserRecord` type, we will add a `UR extends UserRecord` parameter in case we need to create a separate `FacilitatorRecord`.
```java
public class UserBuilder<UR extends UserRecord, U extends User, R, UB extends UserBuilder<UR, U, R, ? super UB>>
```

We've done a lot of work building up to `<U extends User, R, UB extends UserBuilder<U, R, ? super UB>>`.  At each step we added type parameters to make our `UserBuilder`more flexible.  It is easy to add type parameter with the opposite effect.  The type parameters that have been added are special in that they all relax existing type constraints rather than impose new constraints.

Adding this new `UR extends UserRecord` is different in that it is not relaxing an existing constraint but adding a new constraint.  Our builder is now bound to two different incompatible types.

For reasons we'll discuss more below, we will need to change the `U extends User` parameter to be an unbounded `U`, and we're allowed to do that since our `UserBuilder` methods now work with a `UserRecord` instead of a `User`.

```java
public class UserBuilder<UR extends UserRecord, U, R, UB extends UserBuilder<UR, U, R, ? super UB>> {
    protected Consumer<U> setter;
    protected Supplier<R> returnRef;
    protected UR userRecord;
    protected Function<UR,U> factory;
    protected U instance;
    
    public UserBuilder(UR userRecord, Function<UR,U> factory, Consumer<U> setter, Supplier<R> returnRef) {
        this.setter = setter;
        this.returnRef = returnRef;
        this.userRecord = userRecord;
        this.factory = factory;
    }

    public UB setName(String name) {
        userRecord.name = name;
        return (UB)this;
    }

    public UB setDate(Date date) {
        userRecord.date = date;
        return (UB)this;
    }

    public UB setOption(String key, String value){
        userRecord.opts.put(key, value);
        return (UB)this;
    }

    public MembershipBuilder<UB>.MembershipBuilderGroup setPrimaryMembership(){
        return MembershipBuilder.start(m -> userRecord.primaryMembership = m, (UB)this);
    }

    public R finalizeUser(){
        instance = factory.apply(userRecord);
        setter.accept(instance);
        return returnRef.get();
    }
    
    public static <T extends UserBuilder<UserRecord, User, User, ? super T>> T standalone(){
        T builder = (T)new UserBuilder<>(new UserRecord(), (record) -> new User(record), u -> {}, (Supplier<User>)null);
        builder.returnRef = () -> builder.instance;
        return builder;
    }
}
```

The `UserBuilder` now has
```java
protected UR userRecord;
protected Function<UR,U> factory;
```
Both are parameters of the constructor.  This new parameter has further generalized our builder to separate the Type representing the internal state and final built Type passed to the Consumer.

All the setter methods now set values on the `UserRecord` instead of the `User`.  And, it is only in the `finalizeUser` that we use the `factory` Function to convert the `UserRecord` to a `User`.

Our `Builder` is no longer using the `User` type directly, but rather through a `Function` and `Consumer` that are passed in.  This is what allowed use to remove the `U extends User` constraint and keep the `UR` and `U` type parameters compatible and the method signature unchanged.

In this code we are building up a `UserRecord` which can be used to build a `User` as in the `standalone` builder, but it could be used for other purposes.  You may want to simply build the `UserRecord` and hold on to it for later instead of building the `User` object.

```java
public static <T extends UserBuilder<UserRecord, UserRecord, UserRecord, ? super T>> T recordBuilder(){
        ...
}
```

This is the reason we needed to change `U extends User` to be unbounded.  To make this `recordBuilder` we need to set `U` to `UserRecord` which is incompatible with `User`.

You may be wondering when we need to this, but look no further than the `UserRecord`
```java
public class UserRecord {
    public String name;
    public Date date;
    public Map<String,String> opts = new HashMap<>();
    public MembershipRecord primaryMembership;
    public MembershipRecord secondaryMembership;
    public List<MembershipRecord> memberships = new ArrayList<>();
}
```

This holds `MembershipRecord` instead of `Membership`.  Our `MembershipBuilder` will need to be able to build a `MembershipRecord` for the `UserRecord` or a full `Membership` standalone.  We would want to make the same changes to `Roster`, adding a `RosterRecord` which would hold a `FacilitatorRecord` instead of a `Facilitator`.

At this point, what we have is a `UserRecordBuilder` which can become a `UserBuilder` or adopt other properties.

## Deriving the BuilderBase
Deriving a generic `BuilderBase` from our `UserBuilder` is quite easy at this point.  We'll remove the `UR extends UserRecord` constraint and the setters using the `UserRecord` instance.


```java
public class BuilderBase<T, V, R, B extends BuilderBase<T, V, R, ? super B>> {
    protected Supplier<T> instanceRef;
    protected Function<T,V> factory;
    protected Consumer<V> consumer;
    protected Supplier<R> returnRef;
    protected V builtValue = null;

    protected BuilderBase(Supplier<T> instanceRef, Function<T,V> factory, Consumer<V> consumer, Supplier<R> returnRef) {
        this.instanceRef = instanceRef;
        this.factory = factory;
        this.consumer = consumer;
        this.returnRef = returnRef;
    }
    
    protected BuilderBase(BuilderOpts<T,V,R> options) {
        this(options.supplier, options.factory, options.consumer, options.returnSupplier);
        options.builderSetup.accept(this);
    }
    
    protected R finalizeInstance() {
        builtValue = factory.apply(instanceRef.get());
        consumer.accept(builtValue);
        return returnRef.get();
    }
}
```

This is the F-Bound `BuilderBase`.  There is a new constructor taking an F-Bound `BuilderOpts`, this is a simple builder for the `Builder` parameters.  This greatly simplifies creating different types of builders, and avoids the need to pass `null` values and set them after construction as we have been doing.

In our `UserBuilder` we are passing in and holding an instance of a `UserRecord`.  In the `BuilderBase` we have changed `T` to a `Supplier<T>`.  Typically, we are able to create a single instance of a `record` for our builder, and in those cases we can wrap that instance in a `Supplier`.  There can be situations where you may not have a single durable instance.  You may want to modify a particular object accessed through a `getter` which needs to be re-fetched for each use, or you may have some proxy `record` object which requires a series of method calls before or after each use.  In these cases we need the `BuilderBase` to support a `Supplier` for it to work with our existing types.  If the `BuilderBase` did not support a `Supplier`, we would be forced to override and re-write the `BuilderBase` or write a special wrapper class to use as the `record`.

An example of this can be seen in [page-model-tools](https://github.com/pagemodel/page-model-tools), instead of `Builders` it is concerned with fluent `Tester` types following a similar generic type parameter pattern.  In using Selenium to test `WebElements`, a located instance of a `WebElement` can go stale and throw an exception if used.  The `WebElementTester` must re-fetch the `WebElement` instead of holding a single instance to avoid a stale element exception. 


## The Four Generics Types

We began with a concrete `UserBuilder` with no type parameter, this is the first pattern, the trivial type with no parameters.

0. `none` - No Parameters - Concrete Builder

We then made three different version to fix three different problems.  Each problem introduced a specific type parameter.
These are the 4 patterns:
1. `<T extends UserRecord>` - Record holding internal state
2. `<V>` - Output Type
3. `<R>` - Return Value for standalone of chainable builders
4. `<B extends UserBuilder<B>>` - Self Type - F-Polymorphic Builder
    * or `<B extends UserBuilder<? super B>>` - FL-Polymorphic Builder

## Final User, Records, and Builders

Here is our final `BuilderBase` class:
```java
public abstract class BuilderBase<T,V,R,B extends BuilderBase<T,V,R,? super B>> {
	protected Supplier<T> instanceRef;
	protected Function<T,V> factory;
	protected Consumer<V> consumer;
	protected Supplier<R> returnRef;
	protected V builtValue = null;
	protected B self = (B) this;

	protected BuilderBase(Supplier<T> instanceRef, Function<T,V> factory, Consumer<V> consumer, Supplier<R> returnRef) {
		this.instanceRef = instanceRef;
		this.factory = factory;
		this.consumer = consumer;
		this.returnRef = returnRef;
	}

	protected BuilderBase(T instance, Function<T,V> factory, Consumer<V> consumer, Supplier<R> returnRef) {
		this(() -> instance, factory, consumer, returnRef);
	}

	protected BuilderBase(BuilderOpts<T,V,R> options) {
		this(options.supplier, options.factory, options.consumer, options.returnSupplier);
		options.builderSetup.accept(this);
	}
    
	protected R finalizeInstance() {
		builtValue = factory.apply(instanceRef.get());
		consumer.accept(builtValue);
		return returnRef.get();
	}
}
```

Using this as a base class, here is the final `User`, `UserRecord`, and `Builders` together:
```java
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

		public static class Builder extends BaseBuilder<Record,Record,Record,Builder> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder() { this(new Record()); }
			public static class Fluent<R> extends BaseBuilder<Record,Record,R,Fluent<R>> {
				public Fluent(Consumer<Record> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
			}
		}
	}

	public static class BaseBuilder<T extends Record, V, R, B extends BaseBuilder<T,V,R,? super B>> extends BuilderBase<T,V,R,B> {
		public BaseBuilder(BuilderOpts<T,V,R> options) { super(options); }

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

		public Membership.Record.Builder.Fluent<B>.MembershipBuilderGroup setPrimaryMembership(){
			return Membership.Record.Builder.Fluent.start(m -> instanceRef.get().primaryMembership = m, () -> self);
		}

		public Membership.Record.Builder.Fluent<B>.MembershipBuilderGroup setSecondaryMembership(){
			return Membership.Record.Builder.Fluent.start(m -> instanceRef.get().secondaryMembership = m, () -> self);
		}

		public R finalizeUser(){
			return super.finalizeInstance();
		}
	}

	public static class Builder extends BaseBuilder<Record,User,User,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(User::new)); }
		public Builder() { this(new Record()); }
		public static class Fluent<R> extends BaseBuilder<Record,User,R,Fluent<R>> {
			public Fluent(Consumer<User> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder()).asFluent(consumer, returnRef)); }
		}
	}
}
```
This defines `User`, `User.Recored`, `User.BuilderBase`, `User.Builder`, `User.Builder.Fluent`, `User.Record.Builder`, and `User.Record.Builder.Fluent`.  We have 5 different `Builders` covering all of our building needs.

Here is our `Facilitator` extending `User`:
```java
public class Facilitator extends User {
	/* New Facilitator fields go here */

	public Facilitator(Record record) {
		super(record);
		/* Set Facilitator fields from Facilitator.Record here */
	}

	/* New Facilitator methods go here */

	public static class Record extends User.Record {
		/* New Facilitator Record fields go here */

		public static class Builder extends BaseBuilder<Record,Record,Record,Builder> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder() { this(new Record()); }
			public static class Fluent<R> extends BaseBuilder<Record,Record,R,Builder.Fluent<R>> {
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
```
Our `Facilitator` extends `User` and `Facilitator.Record` extends `User.Record`, and just in the same way our `Facilitator.BuilderBase` extends `User.BuilderBase` without needing to override or redefine any code.  We get the same 4 `Builder`, `Builder.Fluent`, `Record.Builder`, and `Record.Builder.Fluent` builder classes that trace back from `Facilitator.BuilderBase` to `User.BuilderBase`.

Here is the `Membership` and builders used by `User` and `Facilitator`:
```java
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
```

We have all the same builders as in `User`, while retaining the *guided* builder behavior with these three inner classes:
```java
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
```

And finally, the `Roster` and its builders:
```java
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
```

Our `Membership`, `User`, `Facilitator`, and `Roster` follow a common pattern for `Record` and `Builder` classes.

The `Record.Builder` can be re-used as a `Record` factory object in place of calling the `Record` constructor directly, and `Builder` can be used as a factory object to create a type from a `Record`.  You can see an example of this in the `Roster` class for working with `Facilitator` and `Membership` records and objects. 

Our `Builder` classes can now follow the same inheritance and composition patterns as the types they build.  And, they are parameterized with the various types involved for use in more abstract type-base dispatching that often result in something like an `AbstractFactoryFactoryFactory` class.


## Analyzing the Effective Builder Pattern

> The following code example is taken from: [https://blogs.oracle.com/javamagazine/post/exploring-joshua-blochs-builder-design-pattern-in-java](https://blogs.oracle.com/javamagazine/post/exploring-joshua-blochs-builder-design-pattern-in-java)
>
> This is an example of Joshua Bloch's **Effective Builder Pattern**
```java
public class Book {
    private final String isbn;
    private final String title;
    private final Genre genre;
    private final String author;
    private final Year published;
    private final String description;
    private Book(Builder builder) {
        this.isbn = builder.isbn;
        this.title = builder.title;
        this.genre = builder.genre;
        this.author = builder.author;
        this.published = builder.published;
        this.description = builder.description;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public Genre getGenre() {
        return genre;
    }

    public String getAuthor() {
        return author;
    }

    public Year getPublished() {
        return published;
    }

    public String getDescription() {
        return description;
    }

    public static class Builder {
        private final String isbn;
        private final String title;
        private Genre genre;
        private String author;
        private Year published;
        private String description;

        public Builder(String isbn, String title) {
            this.isbn = isbn;
            this.title = title;
        }

        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder published(Year published) {
            this.published = published;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }

}
```
This is not too different from versions of our `User` and `UserBuilder`.  We have a `Book` class with some fields set in the constructor.  The `Book.Builder` class has the same fields which are set through a fluent API, and a `build` method to create and return a `User`.

Let's update the `Builder` to inherit from our `BuilderBase` and fill in the types.
```java
public static class Builder extends BuilderBase<BookRecord???, Book, Book, Builder>
```
We don't have a `BookRecord` type, instead the `Builder` has the fields and is the record type itself:
```java
public static class Builder extends BuilderBase<Builder, Book, Book, Builder>
```

Updating the `Builder` to use the `BuilderBase` methods and fields we get:
```java
public static class Builder extends BuilderBase<Builder,Book,Book,Builder> {
    private final String isbn;
    private final String title;
    private Genre genre;
    private String author;
    private Year published;
    private String description;

    public Builder(String isbn, String title) {
        super((Supplier<Builder>) null, r -> new Book(r), b -> {}, null);
        super.instanceRef = () -> this;
        super.returnRef = () -> super.builtInstance;
        this.isbn = isbn;
        this.title = title;
    }

    public Builder genre(Genre genre) {
        this.genre = genre;
        return this;
    }

    public Builder author(String author) {
        this.author = author;
        return this;
    }

    public Builder published(Year published) {
        this.published = published;
        return this;
    }

    public Builder description(String description) {
        this.description = description;
        return this;
    }

    public Book build() {
        return super.finalizeInstance();
    }
}
```
We have to do some juggling in the constructor to set the record `Supplier` and return `Supplier` both to return the `Builder` itself.  This can be simplified using the `BuilderOpts` class.  The `build` method now calls the `finalizeInstance`, we have moved the `Book` constructor call from the `build` method to the `factory` function.

Next, let's look at adding a separate `Record` class and parameterizing a `BaseBuilder`:
```java
public class Book {
    ...
    protected Book(Record record) {
        this.isbn = record.isbn;
        this.title = record.title;
        this.genre = record.genre;
        this.author = record.author;
        this.published = record.published;
        this.description = record.description;
    }
    ...

    public static class Record {
        public String isbn;
        public String title;
        public Genre genre;
        public String author;
        public Year published;
        public String description;
        
        public Record(String isbn, String title){
            this.isbn = isbn;
            this.title = title;
        }
    }
    
    public static class BaseBuilder<T extends Record,V,R,B extends BaseBuilder<T,V,R,? super B>> extends BuilderBase<T,V,R,B> {
        public BaseBuilder(BuilderOpts<T,V,R> options) { super(options); }

        public B genre(Genre genre) {
            instanceRef.get().genre = genre;
            return self;
        }

        public B author(String author) {
            instanceRef.get().author = author;
            return self;
        }

        public B published(Year published) {
            instanceRef.get().published = published;
            return self;
        }

        public B description(String description) {
            instanceRef.get().description = description;
            return self;
        }

        public R build() {
            return super.finalizeInstance();
        }
    }
}
```
There is very little change from the earlier `Builder` to the `Record` and `BaseBuilder`.  The `Builder` constructor parameters for `isbn` and `title` have been moved to the `Record`.  From here we can add the four concrete `Book.Builder` and `Book.Record.Builder` as we did in `User` and `Facilitator`, making small modifications for `isbn` and `title` constructor parameters in the `Record`.

For the `Book.Builder` we can copy the `User.Builder` changing any use of `User` to `Book`, and adding parameters for the `isbn` and `title`:

```java
    public static class Builder extends BaseBuilder<Record,Book,Book,Builder> {
        public Builder(Record record) { super(BuilderOpts.build(record).toValue(Book::new)); }
        public Builder(String isbn, String title) { this(new Record(isbn, title)); }
        public static class Fluent<R> extends BaseBuilder<Record,Book,R,Fluent<R>> {
            public Fluent(String isbn, String title, Consumer<Book> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder(isbn, title)).asFluent(consumer, returnRef)); }
        }
    }
```

This new `Builder` is a complete replacement for the original `Builder` with all expected methods. Any code using the old `Book.Builder` will continue working with this new FBound `Book.Builder`.

For `Book.Record.Builder` we can copy the `User.Record.Builder` with no modification to the types, only adding the `isbn` and `title`:
```java
    public static class Builder extends BaseBuilder<Record,Record,Record,Builder> {
        public Builder(Record record) { super(BuilderOpts.build(record)); }
        public Builder(String isbn, String title) { this(new Record(isbn, title)); }
        public static class Fluent<R> extends BaseBuilder<Record,Record,R, Fluent<R>> {
            public Fluent(String isbn, String title, Consumer<Record> consumer,Supplier<R> returnRef) { super(BuilderOpts.from(new Builder(isbn, title)).asFluent(consumer, returnRef)); }
        }
    }
```

The full `Book` class with the `FBound Builder`:
```java
public class Book {
	private final String isbn;
	private final String title;
	private final Genre genre;
	private final String author;
	private final Year published;
	private final String description;
	protected Book(Record record) {
		this.isbn = record.isbn;
		this.title = record.title;
		this.genre = record.genre;
		this.author = record.author;
		this.published = record.published;
		this.description = record.description;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public Genre getGenre() {
		return genre;
	}

	public String getAuthor() {
		return author;
	}

	public Year getPublished() {
		return published;
	}

	public String getDescription() {
		return description;
	}

	public static class Record {
		public String isbn;
		public String title;
		public Genre genre;
		public String author;
		public Year published;
		public String description;

		public Record(String isbn, String title){
			this.isbn = isbn;
			this.title = title;
		}

		public static class Builder extends BaseBuilder<Record,Record,Record,Builder> {
			public Builder(Record record) { super(BuilderOpts.build(record)); }
			public Builder(String isbn, String title) { this(new Record(isbn, title)); }
			public static class Fluent<R> extends BaseBuilder<Record,Record,R, Fluent<R>> {
				public Fluent(String isbn, String title, Consumer<Record> consumer,Supplier<R> returnRef) { super(BuilderOpts.from(new Builder(isbn, title)).asFluent(consumer, returnRef)); }
			}
		}
	}
	
	public static class BaseBuilder<T extends Record,V,R,B extends BaseBuilder<T,V,R,? super B>> extends BuilderBase<T,V,R,B> {
		public BaseBuilder(BuilderOpts<T,V,R> options) { super(options); }

		public B genre(Genre genre) {
			instanceRef.get().genre = genre;
			return self;
		}

		public B author(String author) {
			instanceRef.get().author = author;
			return self;
		}

		public B published(Year published) {
			instanceRef.get().published = published;
			return self;
		}

		public B description(String description) {
			instanceRef.get().description = description;
			return self;
		}

		public R build() {
			return super.finalizeInstance();
		}
	}

	public static class Builder extends BaseBuilder<Record,Book,Book,Builder> {
		public Builder(Record record) { super(BuilderOpts.build(record).toValue(Book::new)); }
		public Builder(String isbn, String title) { this(new Record(isbn, title)); }
		public static class Fluent<R> extends BaseBuilder<Record,Book,R,Fluent<R>> {
			public Fluent(String isbn, String title, Consumer<Book> consumer, Supplier<R> returnRef) { super(BuilderOpts.from(new Builder(isbn, title)).asFluent(consumer, returnRef)); }
		}
	}
}
```

### Effective Builder and FBound Builder
The `Effective Builder` viewed as an `FBound Builder` is a builder whose record is itself and returns the built value. `BuilderBase<B,V,V,B>`.  In this, we can see that the `FBound Builder` gives us a framework for classifying and speaking about the nuances of other builder approaches.

An `Effective Builder` can be safely and easily converted to an `FBound Builder` with the full range of builder behaviors.

## Fun with Type-Bound Interfaces