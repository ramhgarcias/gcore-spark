package planner.operators

import planner.trees.PlannerTreeNode

/** A column in a physical table. */
case class Column(columnName: String) extends PlannerTreeNode {

  override def name: String = s"${super.name} [$columnName]"
}

object Column {

  /**
    * Fixed column names we expect to find in physical tables, for specific entity types:
    * - Vertex, edge and path tables should contain the [[idColumn]];
    * - Edge and path tables should contain the [[fromIdColumn]] and [[toIdColumn]];
    * - Path tables should contain the [[edgeSeqColumn]].
    */
  val idColumn: Column = Column("id")
  val fromIdColumn: Column = Column("fromId")
  val toIdColumn: Column = Column("toId")
  val edgeSeqColumn: Column = Column("edges")

  /** Other fixed column names as needed by targets. */
  val tableLabelColumn: Column = Column("table_label")
}
