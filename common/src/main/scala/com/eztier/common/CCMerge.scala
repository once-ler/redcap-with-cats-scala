/*
  Author:
  https://stackoverflow.com/questions/37980300/use-shapeless-to-merge-two-instances-of-the-same-case-class
  https://stackoverflow.com/users/5020846/peter-neyens
 */
package com.eztier
package common

import shapeless._
import cats.SemigroupK
import cats.implicits._

trait CCMerge[T] {
  def apply(base: T, CCMerge: T): T
}

trait CCMerge0 {
  implicit def genericCCMerge[A, G <: HList](
    implicit gen: Generic.Aux[A, G], CCMergeG: Lazy[CCMerge[G]]
  ): CCMerge[A] =
    new CCMerge[A] {
      def apply(base: A, CCMerge: A): A =
        gen.from(CCMergeG.value(gen.to(base), gen.to(CCMerge)))
    }
}

object CCMergeSyntax extends CCMerge0 {
  def apply[A](implicit CCMerge: Lazy[CCMerge[A]]): CCMerge[A] = CCMerge.value

  implicit def hnilCCMerge: CCMerge[HNil] =
    new CCMerge[HNil] {
      def apply(base: HNil, CCMerge: HNil): HNil = HNil
    }

  implicit def hconsCCMerge[H, T <: HList](
    implicit CCMergeH: CCMerge[H], CCMergeT: Lazy[CCMerge[T]]
  ): CCMerge[H :: T] =
    new CCMerge[H :: T] {
      def apply(base: H :: T, CCMerge: H :: T): H :: T =
        CCMergeH(base.head, CCMerge.head) :: CCMergeT.value(base.tail, CCMerge.tail)
    }

  /*
    Helper to perform:

    case class Foo(a: Option[Int], b: List[Int], c: Option[Int])
    val base = Foo(None, Nil, Some(0))
    val next = Foo(Some(3), List(4), None)
    base merge next // Foo(Some(3),List(4),Some(0))
   */
  implicit class CCMergeOps[A](val base: A) extends AnyVal {
    def merge(change: A)(implicit CCMerge: Lazy[CCMerge[A]]): A =
      CCMerge.value(base, change)
  }

  /*
    Supprt not only Option/List
    And do: CCMerge[Vector[Int]]
   */
  implicit def semigroupKCCMerge[F[_], A](implicit F: SemigroupK[F]): CCMerge[F[A]] =
    new CCMerge[F[A]] {
      def apply(base: F[A], CCMerge: F[A]): F[A] = F.combineK(CCMerge, base)
    }

}
