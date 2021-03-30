package hu.bme.mit.theta.solver.smtlib.impl.mathsat;

import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Interpolant;
import hu.bme.mit.theta.solver.ItpMarker;
import hu.bme.mit.theta.solver.ItpMarkerTree;
import hu.bme.mit.theta.solver.ItpPattern;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibItpSolver;
import hu.bme.mit.theta.solver.smtlib.solver.interpolation.SmtLibInterpolant;
import hu.bme.mit.theta.solver.smtlib.solver.interpolation.SmtLibItpPattern;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;
import hu.bme.mit.theta.solver.smtlib.solver.binary.SmtLibSolverBinary;
import hu.bme.mit.theta.solver.smtlib.solver.SmtLibSolverException;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.solver.transformer.SmtLibTransformationManager;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class MathSATSmtLibItpSolver extends SmtLibItpSolver<MathSATSmtLibItpMarker> {

    public MathSATSmtLibItpSolver(
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
    public MathSATSmtLibItpMarker createMarker() {
        final var marker = new MathSATSmtLibItpMarker();
        markers.add(marker);
        return marker;
    }

    @Override
    protected void add(final MathSATSmtLibItpMarker marker, final Expr<BoolType> assertion, final Set<ConstDecl<?>> consts, final String term) {
        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert (! %s :interpolation-group %s))", term, marker.getMarkerName()));
    }

    @Override
    public Interpolant getInterpolant(final ItpPattern pattern) {
        checkState(getStatus() == SolverStatus.UNSAT, "Cannot get interpolant if status is not UNSAT.");
        checkArgument(pattern instanceof SmtLibItpPattern);
        @SuppressWarnings("unchecked")
        final var mathsatItpPattern = (SmtLibItpPattern<MathSATSmtLibItpMarker>) pattern;

        final Map<ItpMarker, Expr<BoolType>> itpMap = new HashMap<>();
        buildItpMapFromTree(mathsatItpPattern.getRoot(), itpMap);

        return new SmtLibInterpolant(itpMap);
    }

    @Override
    protected void init() {
        super.init();
        issueGeneralCommand("(set-option :produce-interpolants true)");
    }

    private List<MathSATSmtLibItpMarker> buildItpMapFromTree(final ItpMarkerTree<MathSATSmtLibItpMarker> pattern, final Map<ItpMarker, Expr<BoolType>> itpMap) {
        final List<MathSATSmtLibItpMarker> markers = new ArrayList<>();
        for(final var child : pattern.getChildren()) {
            markers.addAll(buildItpMapFromTree(child, itpMap));
        }
        markers.add(pattern.getMarker());

        solverBinary.issueCommand(String.format("(get-interpolant (%s))", markers.stream().map(MathSATSmtLibItpMarker::getMarkerName).collect(Collectors.joining(" "))));
        final var res = parseItpResponse(solverBinary.readResponse());
        itpMap.put(pattern.getMarker(), termTransformer.toExpr(res, BoolExprs.Bool(), new SmtLibModel(Collections.emptyMap())));

        return markers;
    }

    private String parseItpResponse(final String response) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(response));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        try {
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowExceptionErrorListener());
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowExceptionErrorListener());
            return extractString(parser.term());
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
