package state

trait Monad[F[_]]:
  def unit[A](a: A): F[A]
  extension [A](fa: F[A])
    def flatMap[B](f: A => F[B]): F[B]
    def map[B](f: A => B): F[B] = fa.flatMap(a => unit(f(a)))

case class State[S, A](run: S => (S, A))

given StateMonad[S]: Monad[[A] =>> State[S, A]] with
  def unit[A](a: A): State[S, A] = State(s => (s, a))

  extension [A](fa: State[S, A])
    def flatMap[B](f: A => State[S, B]): State[S, B] =
      State(s =>
        fa.run(s) match
          case (s1, a) => f(a).run(s1)
      )
