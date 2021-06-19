package hu.bme.mit.theta.xcfa.transformation.c;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.xcfa.dsl.gen.CBaseVisitor;
import hu.bme.mit.theta.xcfa.dsl.gen.CParser;
import hu.bme.mit.theta.xcfa.transformation.c.types.CType;
import hu.bme.mit.theta.xcfa.transformation.c.types.Enum;
import hu.bme.mit.theta.xcfa.transformation.c.types.NamedType;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.xcfa.transformation.c.types.CTypeFactory.*;

public class TypeVisitor extends CBaseVisitor<CType> {
	public static final TypeVisitor instance = new TypeVisitor();
	private TypeVisitor(){}

	private static final List<String> standardTypes =
			List.of("int", "char", "long", "short", "void", "float", "double", "unsigned");
	private static final List<String> shorthandTypes =
			List.of("long", "short", "unsigned");


	@Override
	public CType visitDeclarationSpecifiers(CParser.DeclarationSpecifiersContext ctx) {
		return createCType(ctx.declarationSpecifier());
	}

	@Override
	public CType visitDeclarationSpecifiers2(CParser.DeclarationSpecifiers2Context ctx) {
		return createCType(ctx.declarationSpecifier());
	}


	private CType mergeCTypes(List<CType> cTypes) {
		List<CType> enums = cTypes.stream().filter(cType -> cType instanceof Enum).collect(Collectors.toList());
		checkState(enums.size() <= 0, "Declaration cannot contain any enums"); // not supported yet
		List<CType> namedElements = cTypes.stream().filter(cType -> cType instanceof NamedType).collect(Collectors.toList());
		NamedType mainType = (NamedType) namedElements.get(namedElements.size() - 1);
		if (shorthandTypes.contains(mainType.getNamedType())) {
			mainType = NamedType("int");
		} else {
			cTypes.remove(mainType);
		}

		CType type = mainType.apply(cTypes);
		// we didn't get explicit signedness
		if (type.isSigned() == null) {
			if (type instanceof NamedType && ((NamedType) type).getNamedType().contains("char")) {
				System.err.println("WARNING: signedness of the type char is implementation specific. Right now it is interpreted as a signed char.");
			}
			type.setSigned(true);
		}
		return type;
	}

	@Override
	public CType visitSpecifierQualifierList(CParser.SpecifierQualifierListContext ctx) {
		return createCType(ctx);
	}

	private CType createCType(CParser.SpecifierQualifierListContext specifierQualifierListContext) {
		List<CType> cTypes = new ArrayList<>();
		while(specifierQualifierListContext != null) {
			CType qualifierSpecifier = null;
			if(specifierQualifierListContext.typeSpecifier() != null) {
				qualifierSpecifier = specifierQualifierListContext.typeSpecifier().accept(this);
			}
			else if(specifierQualifierListContext.typeQualifier() != null) {
				qualifierSpecifier = specifierQualifierListContext.typeQualifier().accept(this);
			}
			if(qualifierSpecifier != null) cTypes.add(qualifierSpecifier);
			specifierQualifierListContext = specifierQualifierListContext.specifierQualifierList();
		}

		return mergeCTypes(cTypes);
	}

	private CType createCType(List<CParser.DeclarationSpecifierContext> declarationSpecifierContexts) {
		List<CType> cTypes = new ArrayList<>();
		for (CParser.DeclarationSpecifierContext declarationSpecifierContext : declarationSpecifierContexts) {
			CType ctype = declarationSpecifierContext.accept(this);
			if(ctype != null) cTypes.add(ctype);
		}

		return mergeCTypes(cTypes);
	}

	@Override
	public CType visitStorageClassSpecifier(CParser.StorageClassSpecifierContext ctx) {
		switch(ctx.getText()) {
			case "typedef": return Typedef();
			case "extern": return Extern();
			case "static": return null;
			case "auto":
			case "register":
			case "_Thread_local": throw new UnsupportedOperationException("Not yet implemented");
		}
		throw new UnsupportedOperationException("Storage class specifier not expected: " + ctx.getText());
	}

	@Override
	public CType visitTypeSpecifierAtomic(CParser.TypeSpecifierAtomicContext ctx) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public CType visitTypeSpecifierCompound(CParser.TypeSpecifierCompoundContext ctx) {
		return ctx.structOrUnionSpecifier().accept(this);
	}

	@Override
	public CType visitCompoundDefinition(CParser.CompoundDefinitionContext ctx) {
		System.err.println("Warning: CompoundDefinitions are not yet implemented!");
		return NamedType("int");
	}

	@Override
	public CType visitCompoundUsage(CParser.CompoundUsageContext ctx) {
		return NamedType(ctx.structOrUnion().getText() + " " + ctx.Identifier().getText());
	}

	@Override
	public CType visitTypeSpecifierEnum(CParser.TypeSpecifierEnumContext ctx) {
		return ctx.enumSpecifier().accept(this);
	}

	@Override
	public CType visitEnumDefinition(CParser.EnumDefinitionContext ctx) {
		String id = ctx.Identifier() == null ? null : ctx.Identifier().getText();
		Map<String, Optional<Expr<?>>> fields = new LinkedHashMap<>();
		for (CParser.EnumeratorContext enumeratorContext : ctx.enumeratorList().enumerator()) {
			String value = enumeratorContext.enumerationConstant().getText();
			CParser.ConstantExpressionContext expressionContext = enumeratorContext.constantExpression();
			Expr<?> expr = expressionContext == null ? null : null;//expressionContext.accept(null ); // TODO
			fields.put(value, Optional.ofNullable(expr));
		}
		return Enum(id, fields);
	}

	@Override
	public CType visitEnumUsage(CParser.EnumUsageContext ctx) {
		return NamedType("enum " + ctx.Identifier().getText());
	}

	@Override
	public CType visitTypeSpecifierExtension(CParser.TypeSpecifierExtensionContext ctx) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public CType visitTypeSpecifierPointer(CParser.TypeSpecifierPointerContext ctx) {
		CType subtype = ctx.typeSpecifier().accept(this);
		for (Token star : ctx.pointer().stars) {
			subtype.incrementPointer();
		}
		return subtype;
	}

	@Override
	public CType visitTypeSpecifierSimple(CParser.TypeSpecifierSimpleContext ctx) {
		switch (ctx.getText()) {
			case "signed":
				return Signed();
			case "unsigned":
				return Unsigned();
			default:
				return NamedType(ctx.getText());
		}
	}

	@Override
	public CType visitTypeSpecifierTypedefName(CParser.TypeSpecifierTypedefNameContext ctx) {
		Optional<CType> type = TypedefVisitor.instance.getType(ctx.getText());
		if(type.isPresent()) {
			return type.get();
		} else {
			if(standardTypes.contains(ctx.getText())) {
				return NamedType(ctx.getText());
			} else {
				return DeclaredName(ctx.getText());
			}
		}
	}

	@Override
	public CType visitTypeSpecifierTypeof(CParser.TypeSpecifierTypeofContext ctx) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public CType visitTypeQualifier(CParser.TypeQualifierContext ctx) {
		switch(ctx.getText()) {
			case "const": return null;
			case "restrict": throw new UnsupportedOperationException("Not yet implemented!");
			case "volatile": return Volatile();
			case "_Atomic": return Atomic();
		}
		throw new UnsupportedOperationException("Type qualifier " + ctx.getText() + " not expected!");
	}

	@Override
	public CType visitFunctionSpecifier(CParser.FunctionSpecifierContext ctx) {
		return null;
	}

	@Override
	public CType visitAlignmentSpecifier(CParser.AlignmentSpecifierContext ctx) {
		return null;
	}

}
