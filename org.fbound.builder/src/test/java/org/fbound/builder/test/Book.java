package org.fbound.builder.test;

import org.fbound.builder.BuilderBase;
import org.fbound.builder.BuilderOpts;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 > The following code example is taken from: https://blogs.oracle.com/javamagazine/post/exploring-joshua-blochs-builder-design-pattern-in-java
 > This is an example of Joshua Bloch's **Effective Builder Pattern** converted to the **FBound Builder Pattern**
 */
public class Book {
	private final String isbn;
	private final String title;
	private final String genre;
	private final String author;
	private final int published;
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

	public String getGenre() {
		return genre;
	}

	public String getAuthor() {
		return author;
	}

	public int getPublished() {
		return published;
	}

	public String getDescription() {
		return description;
	}

	public static class Record {
		public String isbn;
		public String title;
		public String genre;
		public String author;
		public int published;
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

		public B genre(String genre) {
			instanceRef.get().genre = genre;
			return self;
		}

		public B author(String author) {
			instanceRef.get().author = author;
			return self;
		}

		public B published(int published) {
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
