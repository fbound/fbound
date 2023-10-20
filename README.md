# FBound
Tools for building better APIs.

The FBound project was created out of writing the [FBound Guide to Advanced Fluent APIs](docs/advanced_fluent_apis.md), a guide for advanced uses of generic types.  This guide was written to explain the generic type patterns and techniques I used in creating [Page-Model-Tools](https://github.com/pagemodel/page-model-tools) web testing framework.

## FBound Builder Pattern
The FBound `BuilderBase` serves as a base class for any `Builder` type.  This pattern allows inheritance, chaining, and re-use of your `Builder`.

> `Builder Base` class extended by other Builders:  
> [BuilderBase.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/BuilderBase.java)

> Example `User` class with `Record` and `Builders`:  
> [User.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/test/java/org/fbound/builder/test/User.java)

> Example `Facilitator Builder` extending `User Builder`:  
> [Facilitator.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/test/java/org/fbound/builder/test/Facilitator.java)

> Example `Membership Builder` with "guided build mode":  
> [Membership.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/test/java/org/fbound/builder/test/Membership.java)

> `Extension` marker interface can only be implemented by a `BuilderBase` class making it safe to cast to `BuilderBase`:  
> [Extension.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/extension/Extension.java)

> `ExtensionUtils` provides an `Extension` interface with access to its `BuilderBase` protected fields:  
> [ExtensionUtils.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/ExtensionUtils.java)

> `Accept`, `Apply`, `Edit` extensions which add functional methods to a Builder:  
> [Accept.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/extension/Accept.java), [Apply.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/extension/Apply.java), [Edit.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/main/java/org/fbound/builder/extension/Edit.java)

> Example `Roster Builder` implementing `Accept`, `Apply`, and `Edit` extensions:  
> [Roster.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/test/java/org/fbound/builder/test/Roster.java)

> Example usage of `Membership Builder` in guided and unguided mode:  
> Example usage of `Roster Builder` using other builders and extensions:  
> [BuilderTests.java](https://github.com/fbound/fbound/blob/main/org.fbound.builder/src/test/java/org/fbound/builder/test/BuilderTests.java)
