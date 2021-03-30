package hu.bme.mit.theta.solver.smtlib.impl.princess;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Interpolant;
import hu.bme.mit.theta.solver.ItpMarker;
import hu.bme.mit.theta.solver.ItpMarkerTree;
import hu.bme.mit.theta.solver.ItpPattern;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibItpSolver;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import hu.bme.mit.theta.solver.smtlib.solver.binary.SmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.solver.interpolation.SmtLibInterpolant;
import hu.bme.mit.theta.solver.smtlib.solver.interpolation.SmtLibItpPattern;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;

public final class PrincessSmtLibItpSolver extends SmtLibItpSolver<PrincessSmtLibItpMarker> {
    private final Map<Expr<BoolType>, String> assertionNames = new HashMap<>();
    private static final String assertionNamePattern = "_smtinterpol_assertion_%d";
    private static long assertionCount = 0;

    public PrincessSmtLibItpSolver(
        final SmtLibSymbolTable symbolTable, final SmtLibTransformationManager transformationManager,
        final SmtLibTermTransformer termTransformer, final SmtLibSolverBinary solverBinary
    ) {
        super(symbolTable, transformationManager, termTransformer, solverBinary);
    }

    @Override
    public ItpPattern createTreePattern(final ItpMarkerTree<? extends ItpMarker> root) {
        checkNotNull(root);
        return SmtLibItpPattern.of(root);
    }

    @Override
    public PrincessSmtLibItpMarker createMarker() {
        final var marker = new PrincessSmtLibItpMarker();
        markers.add(marker);
        return marker;
    }

    @Override
    protected void add(final PrincessSmtLibItpMarker marker, final Expr<BoolType> assertion, final Set<ConstDecl<?>> consts, final String term) {
        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);

        final var name = String.format(assertionNamePattern, assertionCount++);
        assertionNames.put(assertion, name);
        issueGeneralCommand(String.format("(assert (! %s :named %s))", term, name));
    }

    @Override
    public Interpolant getInterpolant(final ItpPattern pattern) {
        checkState(getStatus() == SolverStatus.UNSAT, "Cannot get interpolant if status is not UNSAT.");
        checkArgument(pattern instanceof SmtLibItpPattern);
        @SuppressWarnings("unchecked")
        final var princessItpPattern = (SmtLibItpPattern<PrincessSmtLibItpMarker>) pattern;

        final var term = patternToTerm(princessItpPattern.getRoot());

        final List<Expr<BoolType>> itpList = new LinkedList<>();

        solverBinary.issueCommand(String.format("(get-interpolants %s)", term));
        for(final var itp : parseItpResponse(solverBinary.readResponse())) {
            itpList.add(termTransformer.toExpr(itp, BoolExprs.Bool(), new SmtLibModel(Collections.emptyMap())));
        }
        itpList.add(False());

        final Map<ItpMarker, Expr<BoolType>> itpMap = new HashMap<>();
        buildItpMapFormList(princessItpPattern.getRoot(), itpList, itpMap);

        return new SmtLibInterpolant(itpMap);
    }

    private String patternToTerm(final ItpMarkerTree<PrincessSmtLibItpMarker> markerTree) {
        String term;

        final var marker = markerTree.getMarker();
        final var terms = marker.getTerms();
        if(terms.size() == 1) {
            term = assertionNames.get(terms.iterator().next().get1());
        }
        else {
            term = String.format("(and %s)", terms.stream().map(t -> assertionNames.get(t.get1())).collect(Collectors.joining(" ")));
        }

        final var children = markerTree.getChildren();
        for(var i = children.size() - 1; i >= 0; i--) {
            if(i == 0) {
                term = String.format("%s %s", patternToTerm(children.get(i)), term);
            }
            else {
                term = String.format("(%s) %s", patternToTerm(children.get(i)), term);
            }
        }

        return term;
    }

    private void buildItpMapFormList(final ItpMarkerTree<PrincessSmtLibItpMarker> pattern, final List<Expr<BoolType>> itpList, final Map<ItpMarker, Expr<BoolType>> itpMap) {
        for (final ItpMarkerTree<PrincessSmtLibItpMarker> child : pattern.getChildren()) {
            buildItpMapFormList(child, itpList, itpMap);
        }
        final ItpMarker marker = pattern.getMarker();
        final Expr<BoolType> itpExpr = itpList.get(0);
        itpMap.put(marker, itpExpr);
        itpList.remove(0);
    }

    @Override
    protected void init() {
        issueGeneralCommand("(set-option :print-success true)");
        issueGeneralCommand("(set-option :produce-models true)");
        issueGeneralCommand("(set-option :produce-interpolants true)");
        issueGeneralCommand("(set-logic ALL)");
    }

    private List<String> parseItpResponse(final String response) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(response));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        try {
            final var interpolants = new ArrayList<String>();

            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowExceptionErrorListener());
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowExceptionErrorListener());

            for(final var term : parser.get_interpolants_response_smtinterpol().term()) {
                interpolants.add(extractString(term));
            }

            return interpolants;
        }
        catch (Exception e) {
            try {
                throw new SmtLibSolverException(parser.response().general_response_error().reason.getText());
            }
            catch(Exception ex) {
                throw new SmtLibSolverException("Could not parse solver output: " + response, e);
            }
        }
    }

    private static String extractString(final ParserRuleContext ctx) {
        return ctx.start.getInputStream().getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
    }
}
