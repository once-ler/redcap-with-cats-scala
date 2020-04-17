/*
https://github.com/typelevel/cats-mtl/issues/71
*/
package com.eztier
package common

import cats.implicits._
import cats.data.Chain
import cats.{Monad, Monoid}
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.mtl.{DefaultMonadState, MonadState}

trait MonadLog[F[_], L] extends MonadState[F, L] with Serializable {
  val monoid: Monoid[L]

  def log(l: L): F[Unit]

  def clear: F[Unit]

  def flush(f: L => F[Unit]): F[Unit]
}

trait DefaultMonadLog[F[_], L] extends DefaultMonadState[F, L] with MonadLog[F, L] {
  def log(l: L): F[Unit] =
    modify(monoid.combine(_, l))

  def clear: F[Unit] =
    set(monoid.empty)

  def flush(f: L => F[Unit]): F[Unit] =
    monad.flatMap(monad.flatMap(get)(f))(_ => clear)
}

case class ChainState[F[_]: Monad, S](val state: Ref[F, Chain[S]]) extends DefaultMonadLog[F, Chain[S]] {
  override val monad: Monad[F] = Monad[F]
  override def get: F[Chain[S]] = state.get
  override def set(s: Chain[S]): F[Unit] = state.set(s)

  override val monoid: Monoid[Chain[S]] = new Monoid[Chain[S]] {
    override def empty: Chain[S] = Chain.empty[S]
    override def combine(x: Chain[S], y: Chain[S]): Chain[S] = x |+| y
  }
}

object MonadLog {
  def createMonadLog[F[_] :Sync : Monad, L]: F[MonadLog[F, Chain[L]]] = {
    for {
      ref <- Ref.of[F, Chain[L]](Chain.empty[L])
      s = ChainState(ref)
    } yield s
  }
}