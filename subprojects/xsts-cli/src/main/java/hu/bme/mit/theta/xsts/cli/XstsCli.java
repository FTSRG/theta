package hu.bme.mit.theta.xsts.cli;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.algorithm.*;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.abstractor.StopCriterions;
import hu.bme.mit.theta.analysis.expl.*;
import hu.bme.mit.theta.analysis.expr.refinement.*;
import hu.bme.mit.theta.analysis.pred.*;
import hu.bme.mit.theta.analysis.waitlist.PriorityWaitlist;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.*;
import hu.bme.mit.theta.xsts.dsl.XSTSVisitor;
import hu.bme.mit.theta.xsts.dsl.gen.XstsDslLexer;
import hu.bme.mit.theta.xsts.dsl.gen.XstsDslParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

public class XstsCli {

    public static void main(String[] args){
        try {
            XstsDslLexer lexer=new XstsDslLexer(CharStreams.fromFileName("src/test/resources/trafficlight.xsts"));
            CommonTokenStream tokenStream=new CommonTokenStream(lexer);
            XstsDslParser parser=new XstsDslParser(tokenStream);
            XstsDslParser.XstsContext model =parser.xsts();
            XSTSVisitor visitor=new XSTSVisitor();
            visitor.visitXsts(model);
            XSTS xsts=visitor.getXsts();

//            System.out.println(StmtUtils.toExpr(xsts.getEnvAction(), VarIndexing.all(0)).getExprs());
//            System.out.println(StmtUtils.toExpr(xsts.getEnvAction(), VarIndexing.all(0)).getIndexing());
            LTS<XstsState, XstsAction> lts= XstsLts.create(xsts);

            final ItpSolver solver = Z3SolverFactory.getInstace().createItpSolver();
            Logger logger = NullLogger.getInstance();

            final Analysis<XstsState<ExplState>, XstsAction, ExplPrec> analysis = XstsAnalysis
                    .create(ExplAnalysis.create(solver, True()));
            final ArgBuilder<XstsState<ExplState>, XstsAction, ExplPrec> argBuilder = ArgBuilder.create(lts,
                    analysis, s -> ExprUtils.simplify(xsts.getProp(),s.getState().getVal()).equals(True()), true);
            final Abstractor<XstsState<ExplState>, XstsAction, ExplPrec> abstractor = BasicAbstractor
                    .builder(argBuilder)
                    .waitlist(PriorityWaitlist.create(ArgNodeComparators.combine(ArgNodeComparators.targetFirst(), ArgNodeComparators.dfs())))
                    .logger(logger).build();

            Refiner<XstsState<ExplState>, XstsAction, ExplPrec> refiner = null;
            refiner = SingleExprTraceRefiner.create(ExprTraceFwBinItpChecker.create(True(), True(), solver),
                    JoiningPrecRefiner.create(new ItpRefToExplPrec()), logger);


            final SafetyChecker<XstsState<ExplState>, XstsAction, ExplPrec> checker = CegarChecker
                    .create(abstractor, refiner, logger);

            final ExplPrec prec = ExplPrec.of(xsts.getVars());
            System.out.println(prec.getVars());

            SafetyResult res=checker.check(prec);
            if(res.isUnsafe()){
                System.out.println(res.asUnsafe().getTrace());
            }
//            res.getArg().getNodes().forEach(System.out::println);
            System.out.println(res.isSafe());

            
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}
