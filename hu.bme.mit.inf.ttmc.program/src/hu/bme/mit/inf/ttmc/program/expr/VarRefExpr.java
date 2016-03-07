package hu.bme.mit.inf.ttmc.program.expr;

import hu.bme.mit.inf.ttmc.constraint.expr.RefExpr;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.program.decl.VarDecl;

public interface VarRefExpr<DeclType extends Type> extends RefExpr<DeclType, VarDecl<DeclType>> {

}
