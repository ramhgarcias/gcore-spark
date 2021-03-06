/*
 * gcore-spark is the reference implementation of the G-CORE graph query
 * language by the Linked Data Benchmark Council (LDBC) - ldbcouncil.org
 *
 * The copyrights of the source code in this file belong to:
 * - CWI (www.cwi.nl), 2017-2018
 *
 * This software is released in open source under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package algebra.operators

import algebra.expressions.Reference

/** A relational join-like operation. */
abstract class JoinLike(lhs: RelationLike,
                        rhs: RelationLike,
                        bindingTable: Option[BindingSet])
  extends BinaryOperator(lhs, rhs, bindingTable) {

  /**
    * Returns all the bindings that appear in at least two binding sets that have been seen so far
    * by this [[JoinLike]].
    */
  def commonInSeenBindingSets: Set[Reference] = BindingSet.intersectBindingSets(seenBindingSets)

  /** The sets of bindings that have been seen so far in this [[JoinLike]] subtree. */
  val seenBindingSets: Seq[Set[Reference]] = {
    var union: Seq[Set[Reference]] = Seq.empty

    lhs match {
      case joinLike: JoinLike => union = union ++ joinLike.seenBindingSets
      case _ => union = union ++ Set(lhs.getBindingSet.refSet)
    }

    rhs match {
      case joinLike: JoinLike => union = union ++ joinLike.seenBindingSets
      case _ => union = union ++ Set(rhs.getBindingSet.refSet)
    }

    union
  }
}

case class LeftOuterJoin(lhs: RelationLike,
                         rhs: RelationLike,
                         bindingTable: Option[BindingSet] = None)
  extends JoinLike(lhs, rhs, bindingTable)

case class InnerJoin(lhs: RelationLike,
                     rhs: RelationLike,
                     bindingTable: Option[BindingSet] = None)
  extends JoinLike(lhs, rhs, bindingTable)

case class CrossJoin(lhs: RelationLike,
                     rhs: RelationLike,
                     bindingTable: Option[BindingSet] = None)
  extends JoinLike(lhs, rhs, bindingTable)
