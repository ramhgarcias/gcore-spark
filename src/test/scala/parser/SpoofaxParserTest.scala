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

package parser

import algebra.exceptions._
import algebra.expressions.{Label, PropertyKey}
import algebra.trees.AlgebraTreeNode
import algebra.types._
import common.exceptions.UnsupportedOperation
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import parser.trees.ParseContext
import schema._

/**
  * Tests the parsing pipeline.
  *
  * Tests the exception throwing behavior of the [[AlgebraTreeNode]]s in the presence of semantic
  * errors in the queries.
  */
class SpoofaxParserTest extends FunSuite
  with BeforeAndAfter
  with Matchers {

  val peopleVertexSchema: EntitySchema =
    EntitySchema(SchemaMap(Map(
      Label("person") -> SchemaMap(Map(
        PropertyKey("id") -> GcoreInteger,
        PropertyKey("age") -> GcoreInteger,
        PropertyKey("height") -> GcoreDecimal,
        PropertyKey("name") -> GcoreString)
      ),
      Label("city") -> SchemaMap(Map(
        PropertyKey("id") -> GcoreInteger,
        PropertyKey("population") -> GcoreInteger,
        PropertyKey("name") -> GcoreString)
      ))
    ))

  val peopleEdgeSchema: EntitySchema =
    EntitySchema(SchemaMap(Map(
      Label("livesIn") -> SchemaMap(Map(
        PropertyKey("id") -> GcoreInteger,
        PropertyKey("fromId") -> GcoreInteger,
        PropertyKey("toId") -> GcoreInteger,
        PropertyKey("since") -> GcoreDate)
      ))
    ))

  /** Graph to test on. All the checks are at schema level, so we do not define data. */
  val peopleGraph: PathPropertyGraph = new PathPropertyGraph {
    override type StorageType = Nothing

    override def graphName: String = "people"

    override def vertexSchema: EntitySchema = peopleVertexSchema
    override def edgeSchema: EntitySchema = peopleEdgeSchema
    override def pathSchema: EntitySchema = EntitySchema.empty

    override def pathData: Seq[Table[StorageType]] = Seq.empty
    override def vertexData: Seq[Table[StorageType]] = Seq.empty
    override def edgeData: Seq[Table[StorageType]] = Seq.empty

    override def edgeRestrictions: SchemaMap[Label, (Label, Label)] = SchemaMap.empty
    override def storedPathRestrictions: SchemaMap[Label, (Label, Label)] = SchemaMap.empty
  }

  val catalog: Catalog = new Catalog { override type StorageType = Nothing }
  val parseContext: ParseContext = ParseContext(catalog = catalog)
  val parser: SpoofaxParser = SpoofaxParser(parseContext)

  after {
    catalog.unregisterGraph("people")
  }

  test("Conjunction of labels is not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v:foo:bar)"
    }
  }

  test("Disjunction of labels is not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v:foo|bar)"
    }
  }

  test("Undirected edges are not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v)-(w)"
    }
  }

  test("Both in- and out- directed edges are not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v)<->(w)"
    }
  }

  test("QueryGraph is not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v) ON (CONSTRUCT () MATCH (w))"
    }
  }

  // TODO: Unignore the trees/SpoofaxToAlgebraMatch "isObj = false for virtual path" test once
  // virtual paths get supported.
  test("Virtual paths are not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v)-/ /->(w)"
    }
  }

  test("NamedGraph exists in the database") {
    assert(!catalog.hasGraph("people"))
    the [NamedGraphNotAvailableException] thrownBy {
      parser parse "CONSTRUCT () MATCH (v) ON people"
    } should matchPattern {
      case NamedGraphNotAvailableException("people") =>
    }
  }

  test("DefaultGraph is registered with the database") {
    assert(!catalog.hasDefaultGraph)
    assertThrows[DefaultGraphNotAvailableException] {
      parser parse "CONSTRUCT () MATCH (v)"
    }
  }

  test("Disjunct labels are present in schema") {
    catalog.registerGraph(peopleGraph)

    the [DisjunctLabelsException] thrownBy {
      parser parse "CONSTRUCT () MATCH (v:foo) ON people"
    } should matchPattern {
      case DisjunctLabelsException("people", Seq(Label("foo")), `peopleVertexSchema`) =>
    }

    the [DisjunctLabelsException] thrownBy {
      parser parse "CONSTRUCT () MATCH ()-[:foo]->() ON people"
    } should matchPattern {
      case DisjunctLabelsException("people", Seq(Label("foo")), `peopleEdgeSchema`) =>
    }

    noException should be thrownBy {
      parser parse "CONSTRUCT () MATCH (p:person) ON people"
    }

    noException should be thrownBy {
      parser parse "CONSTRUCT () MATCH (:person)-[:livesIn]->(:city) ON people"
    }

//    TODO: Uncomment once label disjunction is supported again.
//    noException should be thrownBy {
//      parser parse "CONSTRUCT () MATCH (:person|city) ON people"
//    }
  }

  // TODO: Un-ignore once property unrolling is supported.
  ignore("Property keys are present in schema") {
    catalog.registerGraph(peopleGraph)

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH (:person {foo = 1, bar = 2}) ON people"
    } should matchPattern {
      case PropKeysException("people",
                             Seq(PropertyKey("foo"), PropertyKey("bar")),
                             `peopleVertexSchema`) =>
    }

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH (:person {since = 4}) ON people"
    } should matchPattern {
      case PropKeysException("people", Seq(PropertyKey("since")), `peopleVertexSchema`) =>
    }

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH ()-[{baz = 3}]->() ON people"
    } should matchPattern {
      case PropKeysException("people", Seq(PropertyKey("baz")), `peopleEdgeSchema`) =>
    }

    noException should be thrownBy {
      parser parse "" +
        "CONSTRUCT () " +
        "MATCH (:person {age > 24})-[:livesIn {since = 2018}]-(:city {name = Amsterdam}) ON people"
    }

    noException should be thrownBy {
      parser parse "" +
        "CONSTRUCT () " +
        "MATCH ({age > 24})-[{since = 2018}]-({name = Amsterdam}) ON people"
    }
  }
}
