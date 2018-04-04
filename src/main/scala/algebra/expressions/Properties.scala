package algebra.expressions

import algebra.exceptions.PropKeysException
import algebra.trees.{PropertyContext, SemanticCheckWithContext}
import algebra.types.GcoreString
import common.compiler.Context
import schema.EntitySchema

import scala.collection.mutable

case class PropertyKey(key: String) extends AlgebraExpression {
  children = List(Literal(key, GcoreString()))

  override def name: String = key
}

case class PropertyRef(ref: Reference, propKey: PropertyKey) extends AlgebraExpression {
  override def name: String = s"${super.name} [${ref.refName}.${propKey.key}]"
}

/** A predicate that asserts that the graph entity satisfies all the given property conditions. */
case class WithProps(propConj: AlgebraExpression) extends AlgebraExpression
  with SemanticCheckWithContext {

  children = List(propConj)

  override def checkWithContext(context: Context): Unit = {
    val withLabels = context.asInstanceOf[PropertyContext].labelsExpr
    val schema: EntitySchema = context.asInstanceOf[PropertyContext].schema

    val propKeys: Seq[PropertyKey] = {
      val pks = new mutable.ArrayBuffer[PropertyKey]()
      propConj.forEachDown {
        case pk: PropertyKey => pks += pk
        case _ =>
      }
      pks
    }

    val expectedProps: Seq[PropertyKey] = {
      if (withLabels.isDefined) {
        val labels: Seq[Label] = {
          val ls = new mutable.ArrayBuffer[Label]()
          withLabels.get.forEachDown {
            case l: Label => ls += l
            case _ =>
          }
          ls
        }
        labels.flatMap(label => schema.properties(label))
      } else
        schema.properties
    }

    val unavailablePropKeys: Seq[PropertyKey] = propKeys filterNot expectedProps.contains
    if (unavailablePropKeys.nonEmpty)
      throw
        PropKeysException(
          graphName = context.asInstanceOf[PropertyContext].graphName,
          unavailableProps = unavailablePropKeys,
          schema = schema)
  }
}
