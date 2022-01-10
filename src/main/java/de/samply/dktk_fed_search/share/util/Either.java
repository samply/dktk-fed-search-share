package de.samply.dktk_fed_search.share.util;

import de.samply.dktk_fed_search.share.util.Either.Left;
import de.samply.dktk_fed_search.share.util.Either.Right;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A union type of either a Left or Right value.
 *
 * <p>Usually Right denotes the happy path and Left denotes the exception case.
 *
 * @param <L> component type of the left value
 * @param <R> component type of the right value
 */
public sealed interface Either<L, R> permits Left, Right {

  static <L, R> Either<L, R> left(L val) {
    return new Left<>(val);
  }

  static <L> Either<L, Void> right() {
    return new Right<>(null);
  }

  static <L, R> Either<L, R> right(R val) {
    return new Right<>(val);
  }

  /**
   * Tries to get a value from {@code supplier}.
   *
   * @param supplier the supplier to get the value from
   * @param <R>      component type of the right value
   * @return a Right with the value from the supplier or a Left with the exception thrown
   */
  static <R> Either<Exception, R> tryGet(Supplier<? extends R> supplier) {
    try {
      return new Right<>(supplier.get());
    } catch (Exception e) {
      return new Left<>(e);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  static <L, R> Either<L, R> fromOptional(Optional<? extends R> optional, L left) {
    return optional.isPresent() ? new Right<>(optional.get()) : new Left<>(left);
  }

  /**
   * Maps the value of this Either if it is a Right, performs no operation if this is a Left.
   *
   * @param f   the mapping function
   * @param <U> component type of the mapped right value
   * @return a mapped Right Either or an unchanged Left Either
   */
  <U> Either<L, U> map(Function<? super R, ? extends U> f);

  <U> Either<L, U> flatMap(Function<? super R, ? extends Either<L, U>> f);

  <U> Either<U, R> mapLeft(Function<? super L, ? extends U> f);

  R orElseGet(Function<? super L, ? extends R> f);

  <X extends Throwable> R orElseThrow(Function<? super L, X> f) throws X;

  Either<L, R> orElse(Function<? super L, ? extends Either<L, R>> f);

  /**
   * The container of the left value.
   *
   * @param <L> component type of the left value
   * @param <R> component type of the right value
   */
  record Left<L, R>(L val) implements Either<L, R> {

    @Override
    @SuppressWarnings("unchecked")
    public <U> Either<L, U> map(Function<? super R, ? extends U> f) {
      return (Either<L, U>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Either<L, U> flatMap(Function<? super R, ? extends Either<L, U>> f) {
      return (Either<L, U>) this;
    }

    @Override
    public <U> Either<U, R> mapLeft(Function<? super L, ? extends U> f) {
      Objects.requireNonNull(f);
      return new Left<>(f.apply(val));
    }

    @Override
    public R orElseGet(Function<? super L, ? extends R> f) {
      return f.apply(val);
    }

    @Override
    public <X extends Throwable> R orElseThrow(Function<? super L, X> f) throws X {
      throw f.apply(val);
    }

    @Override
    public Either<L, R> orElse(Function<? super L, ? extends Either<L, R>> f) {
      return f.apply(val);
    }
  }

  /**
   * The container of the right value.
   *
   * @param <L> component type of the left value
   * @param <R> component type of the right value
   */
  record Right<L, R>(R val) implements Either<L, R> {

    @Override
    public <U> Either<L, U> map(Function<? super R, ? extends U> f) {
      Objects.requireNonNull(f);
      return new Right<>(f.apply(val));
    }

    @Override
    public <U> Either<L, U> flatMap(Function<? super R, ? extends Either<L, U>> f) {
      Objects.requireNonNull(f);
      return f.apply(val);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Either<U, R> mapLeft(Function<? super L, ? extends U> f) {
      return (Either<U, R>) this;
    }

    @Override
    public R orElseGet(Function<? super L, ? extends R> f) {
      return val;
    }

    @Override
    public <X extends Throwable> R orElseThrow(Function<? super L, X> f) {
      return val;
    }

    @Override
    public Either<L, R> orElse(Function<? super L, ? extends Either<L, R>> f) {
      return this;
    }
  }
}
