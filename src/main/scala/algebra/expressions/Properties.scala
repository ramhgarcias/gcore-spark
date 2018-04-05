package algebra.expressions

import algebra.exceptions.PropKeysException
import algebra.trees.{PropertyContext, SemanticCheck, SemanticCheckWithContext}
import common.compiler.Context
import common.exceptions.UnsupportedOperation
import schema.EntitySchema

import scala.collection.mutable

/**
  * An available property of an entity. It is the name of the property, rather than its value. A set
  * of properties is restricted to a certain [[Label]].
  */
case class PropertyKey(key: String) extends AlgebraExpression {
  children = List(StringLiteral(key))

  override def name: String = key
}

/**
  * The access of a property's value for a certain variable.
  *
  * For example, if we are interested in the property "name" of a node p labeled "Person", we would
  * write p.name. In this case, "p" is the [[Reference]] of the variable and "name" is the
  * [[PropertyKey]] we are interested in.
  */
case class PropertyRef(ref: Reference, propKey: PropertyKey) extends AlgebraExpression {
  override def name: String = s"${super.name} [${ref.refName}.${propKey.key}]"
}

/**
  * An [[ObjectPattern]] predicate that allows for multi-value property expansion. This will unroll
  * multi-valued properties into individual bindings, such that direct equality comparison can be
  * performed to check if a single value is in the property's value set.
  */
case class WithProps(propConj: AlgebraExpression) extends AlgebraExpression
  with SemanticCheckWithContext with SemanticCheck {

  children = List(propConj)

  /**
    * [[PropertyKey]]s used in this pattern must (1) be present in the graph and (2) correspond to
    * the variable's label(s) or, if the variable has not been restricted to a label (with the
    * [[WithLabels]] predicate), to that variable's entity type (vertex, edge or path).
    */
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

  /** Property unrolling is not supported in the current version of the interpreter. */
  override def check(): Unit =
    throw UnsupportedOperation("Property unrolling is not supported in object pattern.")
}
