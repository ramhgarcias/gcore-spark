package schema

import org.scalatest.FunSuite

class GraphDbTest extends FunSuite {

  val graph1: PartialGraph = new PartialGraph {
    override def graphName: String = "graph1"
  }

  val graph2: PartialGraph = new PartialGraph {
    override def graphName: String = "graph2"
  }

  test("allGraphs") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.registerGraph(graph2)
    assert(graphDb.allGraphs == Seq(graph1, graph2))
  }

  test("hasGraph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.registerGraph(graph2)
    assert(graphDb.hasGraph("graph1"))
    assert(graphDb.hasGraph("graph2"))
  }

  test("graph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.registerGraph(graph2)
    assert(graphDb.graph("graph1") == graph1)
    assert(graphDb.graph("graph2") == graph2)
  }

  test("unregisterGraph(graphName: String) for non-default graph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.unregisterGraph("graph1")
    assert(graphDb.allGraphs == Seq.empty)
  }

  test("unregisterGraph(graphName: String) for default graph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.setDefaultGraph("graph1")
    graphDb.unregisterGraph("graph1")
    assert(graphDb.allGraphs == Seq.empty)
  }

  test("hasDefaultGraph and defaultGraph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    assert(!graphDb.hasDefaultGraph)
    assert(graphDb.defaultGraph().isEmpty)

    graphDb.setDefaultGraph("graph1")
    assert(graphDb.hasDefaultGraph)
    assert(graphDb.defaultGraph() == graph1)

    graphDb.unregisterGraph(graph1)
    assert(!graphDb.hasDefaultGraph)
    assert(graphDb.defaultGraph().isEmpty)
  }

  test("setDefaultGraph throws exception if graph is not registered") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    assert(graphDb.allGraphs.isEmpty)

    assertThrows[SchemaException] {
      graphDb.setDefaultGraph("graph1")
    }
  }

  test("resetDefaultGraph") {
    val graphDb: GraphDb[Seq[Int]] = new GraphDb[Seq[Int]] {}
    graphDb.registerGraph(graph1)
    graphDb.setDefaultGraph("graph1")
    graphDb.resetDefaultGraph()
    assert(graphDb.allGraphs == Seq(graph1))
    assert(!graphDb.hasDefaultGraph)
  }

  /**
    * Graphs used in this test suite. We are not interested in schema or data, but it is not the
    * empty graph either.
    */
  sealed abstract class PartialGraph extends PathPropertyGraph[Seq[Int]] {
    override def vertexSchema: EntitySchema = EntitySchema.empty
    override def pathSchema: EntitySchema = EntitySchema.empty
    override def edgeSchema: EntitySchema = EntitySchema.empty
    override def vertexData: Seq[Table[Seq[Int]]] = Seq.empty
    override def edgeData: Seq[Table[Seq[Int]]] = Seq.empty
    override def pathData: Seq[Table[Seq[Int]]] = Seq.empty
  }
}