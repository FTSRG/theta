package hu.bme.mit.theta.solver.smtlib.impl.z3;

import hu.bme.mit.theta.common.Tuple2;
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

import java.util.Collection;
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

public final class Z3SmtLibItpSolver extends SmtLibItpSolver<Z3SmtLibItpMarker> {
    private boolean topMostContainsAssertions = false;

    public Z3SmtLibItpSolver(
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
    public Z3SmtLibItpMarker createMarker() {
        final var marker = new Z3SmtLibItpMarker();
        markers.add(marker);
        return marker;
    }



    @Override
    public void add(ItpMarker marker, Expr<BoolType> assertion) {
        if(topMostContainsAssertions) {
            issueGeneralCommand("(pop 1)"); // Topmost frame contains marker assertions
            topMostContainsAssertions = false;
        }
        super.add(marker, assertion);
    }

    @Override
    protected void add(final Z3SmtLibItpMarker marker, final Expr<BoolType> assertion, final Set<ConstDecl<?>> consts, final String term) {
        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
    }

    @Override
    public SolverStatus check() {
        if(topMostContainsAssertions) {
            issueGeneralCommand("(pop 1)"); // Topmost frame contains marker assertions
            topMostContainsAssertions = false;
        }
        Z3SmtLibItpMarker.resetMarkerCount();
        issueGeneralCommand("(push 1)"); // Topmost frame contains marker assertions
        topMostContainsAssertions = true;

        for(final var marker : markers.toCollection()) {
            final var terms = marker.getTerms();
            if(terms.size() == 0) {
                issueGeneralCommand(String.format("(assert (! true :named %s))", marker.getMarkerName()));
            }
            else {
                final var term = String.format("(and %s)", String.join(" ", marker.getTerms().stream().map(Tuple2::get2).collect(Collectors.toUnmodifiableList())));

                issueGeneralCommand(String.format("(assert (! %s :named %s))", term, marker.getMarkerName()));
            }
        }

        return super.check();
    }

    @Override
    public void push() {
        if(topMostContainsAssertions) {
            issueGeneralCommand("(pop 1)"); // Topmost frame contains marker assertions
            topMostContainsAssertions = false;
        }
        super.push(); // Topmost frame contains marker assertions
    }

    @Override
    public void pop(int n) {
        if(topMostContainsAssertions) {
            issueGeneralCommand("(pop 1)"); // Topmost frame contains marker assertions
            topMostContainsAssertions = false;
        }
        super.pop(n); // Topmost frame contains marker assertions
    }

    @Override
    public Interpolant getInterpolant(final ItpPattern pattern) {
        checkState(getStatus() == SolverStatus.UNSAT, "Cannot get interpolant if status is not UNSAT.");
        checkArgument(pattern instanceof SmtLibItpPattern);
        @SuppressWarnings("unchecked")
        final var z3ItpPattern = (SmtLibItpPattern<Z3SmtLibItpMarker>) pattern;

        final var term = patternToTerm(z3ItpPattern.getRoot());
        final var markerCount = getMarkerCount(z3ItpPattern.getRoot());

        final List<Expr<BoolType>> itpList = new LinkedList<>();

        solverBinary.issueCommand(String.format("(get-interpolant %s)", term));
        for(var i = 0; i < markerCount; i++) {
            final var res = parseItpResponse(solverBinary.readResponse());
            itpList.add(termTransformer.toExpr(res, BoolExprs.Bool(), new SmtLibModel(Collections.emptyMap())));
        }
        // itpList.add(False());

        final Map<ItpMarker, Expr<BoolType>> itpMap = new HashMap<>();
        buildItpMapFormList(z3ItpPattern.getRoot(), itpList, itpMap);

        return new SmtLibInterpolant(itpMap);
    }

    private String patternToTerm(final ItpMarkerTree<Z3SmtLibItpMarker> markerTree) {
        final Collection<String> opTerms = new LinkedList<>();

        final Z3SmtLibItpMarker marker = markerTree.getMarker();
        opTerms.add(marker.getMarkerName());

        for (final var child : markerTree.getChildren()) {
            final var childTerm = patternToTerm(child);
            opTerms.add(childTerm);
        }

        return String.format("(interp (and %s))", String.join(" ", opTerms));
    }

    private void buildItpMapFormList(final ItpMarkerTree<Z3SmtLibItpMarker> pattern, final List<Expr<BoolType>> itpList,
                                     final Map<ItpMarker, Expr<BoolType>> itpMap) {
        for (final ItpMarkerTree<Z3SmtLibItpMarker> child : pattern.getChildren()) {
            buildItpMapFormList(child, itpList, itpMap);
        }
        final ItpMarker marker = pattern.getMarker();
        final Expr<BoolType> itpExpr = itpList.get(0);
        itpMap.put(marker, itpExpr);
        itpList.remove(0);
    }

    private int getMarkerCount(final ItpMarkerTree<Z3SmtLibItpMarker> markerTree) {
        return 1 + markerTree.getChildren().stream().mapToInt(this::getMarkerCount).sum();
    }

    @Override
    protected void init() {
        issueGeneralCommand("(set-option :print-success true)");
        issueGeneralCommand("(set-option :produce-interpolants true)");
        super.init();
        issueGeneralCommand("(push 1)"); // Topmost frame contains marker assertions
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
