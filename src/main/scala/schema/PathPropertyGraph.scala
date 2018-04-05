package schema
import algebra.expressions.Label

/**
  * The path property graph is the queryable unit of G-CORE. The graph retains information about
  * vertices, edges and paths. The stored information refers to labels and key-value attributes
  * of an entity. A graph has a [[GraphSchema]], that describes its structure, and stored
  * [[GraphData]].
  */
abstract class PathPropertyGraph extends GraphSchema with GraphData {

  def graphName: String

  def isEmpty: Boolean = false

  def nonEmpty: Boolean = !isEmpty

  override def toString: String = schemaString

  def schemaString: String =
    s"\nGraph: $graphName\n" +
      s"[*] Vertex schema:\n$vertexSchema" +
      s"[*] Edge schema:\n$edgeSchema" +
      s"[*] Path schema:\n$pathSchema"

  def sparkSchemaString: String =
    s"\nGraph: $graphName\n" +
      s"[*] Vertex schema:\n$vertexData\n" +
      s"[*] Edge schema:\n$edgeData\n" +
      s"[*] Path schema:\n$pathData"
}

object PathPropertyGraph {

  val empty: PathPropertyGraph = new PathPropertyGraph {
    override def isEmpty: Boolean = true

    override def graphName: String = "PathPropertyGraph.empty"
    override def vertexData: Seq[Table[StorageType]] = Seq.empty
    override def edgeData: Seq[Table[StorageType]] = Seq.empty
    override def pathData: Seq[Table[StorageType]] = Seq.empty
    override def vertexSchema: EntitySchema = EntitySchema.empty
    override def pathSchema: EntitySchema = EntitySchema.empty
    override def edgeSchema: EntitySchema = EntitySchema.empty

    override def edgeRestrictions: SchemaMap[Label, (Label, Label)] = SchemaMap.empty
    override def storedPathRestrictions: SchemaMap[Label, (Label, Label)] = SchemaMap.empty
  }
}
