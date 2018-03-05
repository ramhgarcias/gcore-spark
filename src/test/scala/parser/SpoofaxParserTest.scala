package parser

import algebra.exceptions._
import algebra.expressions.{Label, PropertyKey}
import algebra.trees.AlgebraTreeNode
import algebra.types._
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

  type GraphType = Nothing

  val peopleVertexSchema: EntitySchema =
    EntitySchema(SchemaMap(Map(
      Label("person") -> SchemaMap(Map(
        PropertyKey("id") -> TypeInteger(),
        PropertyKey("age") -> TypeInteger(),
        PropertyKey("height") -> TypeDecimal(),
        PropertyKey("name") -> TypeString())
      ),
      Label("city") -> SchemaMap(Map(
        PropertyKey("id") -> TypeInteger(),
        PropertyKey("population") -> TypeInteger(),
        PropertyKey("name") -> TypeString())
      ))
    ))

  val peopleEdgeSchema: EntitySchema =
    EntitySchema(SchemaMap(Map(
      Label("livesIn") -> SchemaMap(Map(
        PropertyKey("id") -> TypeInteger(),
        PropertyKey("fromId") -> TypeInteger(),
        PropertyKey("toId") -> TypeInteger(),
        PropertyKey("since") -> TypeDate())
      ))
    ))

  /** Graph to test on. All the checks are at schema level, so we do not define data. */
  val peopleGraph: PathPropertyGraph[GraphType] = new PathPropertyGraph[GraphType] {
    override def graphName: String = "people"

    override def vertexSchema: EntitySchema = peopleVertexSchema
    override def edgeSchema: EntitySchema = peopleEdgeSchema
    override def pathSchema: EntitySchema = EntitySchema.empty

    override def pathData: Seq[Table[GraphType]] = Seq.empty
    override def vertexData: Seq[Table[GraphType]] = Seq.empty
    override def edgeData: Seq[Table[GraphType]] = Seq.empty
  }

  val graphDb: GraphDb[GraphType] = new GraphDb[GraphType] {}
  val parseContext: ParseContext = ParseContext(graphDb = graphDb)
  val parser: SpoofaxParser = SpoofaxParser(parseContext)

  after {
    graphDb.unregisterGraph("people")
  }

  test("Conjunction of labels is not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v:foo:bar)"
    }
  }

  test("QueryGraph is not supported yet") {
    assertThrows[UnsupportedOperation] {
      parser parse "CONSTRUCT () MATCH (v) ON (CONSTRUCT () MATCH (w))"
    }
  }

  test("NamedGraph exists in the database") {
    assert(!graphDb.hasGraph("people"))
    the [NamedGraphNotAvailableException] thrownBy {
      parser parse "CONSTRUCT () MATCH (v) ON people"
    } should matchPattern {
      case NamedGraphNotAvailableException("people") =>
    }
  }

  test("DefaultGraph is registered with the database") {
    assert(!graphDb.hasDefaultGraph)
    assertThrows[DefaultGraphNotAvailableException] {
      parser parse "CONSTRUCT () MATCH (v)"
    }
  }

  test("Disjunct labels are present in schema") {
    graphDb.registerGraph(peopleGraph)

    the [DisjunctLabelsException] thrownBy {
      parser parse "CONSTRUCT () MATCH (v:foo) ON people"
    } should matchPattern {
      case DisjunctLabelsException("people", Seq(Label("foo")), peopleVertexSchema) =>
    }

    the [DisjunctLabelsException] thrownBy {
      parser parse "CONSTRUCT () MATCH ()-[:foo]-() ON people"
    } should matchPattern {
      case DisjunctLabelsException("people", Seq(Label("foo")), peopleEdgeSchema) =>
    }

    noException should be thrownBy {
      parser parse "CONSTRUCT () MATCH (p:person) ON people"
    }

    noException should be thrownBy {
      parser parse "CONSTRUCT () MATCH (:person)-[:livesIn]-(:city) ON people"
    }

    noException should be thrownBy {
      parser parse "CONSTRUCT () MATCH (:person|city) ON people"
    }
  }

  test("Property keys are present in schema") {
    graphDb.registerGraph(peopleGraph)

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH (:person {foo = 1, bar = 2}) ON people"
    } should matchPattern {
      case PropKeysException("people",
                             Seq(PropertyKey("foo"), PropertyKey("bar")),
                             peopleVertexSchema) =>
    }

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH (:person {since = 4}) ON people"
    } should matchPattern {
      case PropKeysException("people", Seq(PropertyKey("since")), peopleVertexSchema) =>
    }

    the [PropKeysException] thrownBy {
      parser parse "CONSTRUCT () MATCH ()-[{baz = 3}]-() ON people"
    } should matchPattern {
      case PropKeysException("people", Seq(PropertyKey("baz")), peopleEdgeSchema) =>
    }

//    TODO: Uncomment once expressions are fixed.
//
//    noException should be thrownBy {
//      parser parse "" +
//        "CONSTRUCT () " +
//        "MATCH (:person {age > 24})-[:livesIn {since = 2018}]-(:city {name = Amsterdam}) ON people"
//    }
//
//    noException should be thrownBy {
//      parser parse "" +
//        "CONSTRUCT () " +
//        "MATCH ({age > 24})-[{since = 2018}]-({name = Amsterdam}) ON people"
//    }
  }
}