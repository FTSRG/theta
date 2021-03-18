package hu.bme.mit.theta.xcfa;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.xcfa.dsl.XcfaDslManager;
import hu.bme.mit.theta.xcfa.ir.InstructionHandler;
import hu.bme.mit.theta.xcfa.ir.LlvmIrProvider;
import hu.bme.mit.theta.xcfa.ir.SSAProvider;

import java.io.*;
import java.util.*;

import static hu.bme.mit.theta.xcfa.ir.Utils.*;

@SuppressWarnings("unused")
public class XcfaUtils {
    /*
     * Creates an XCFA from the specified file.
     * This is the recommended method for getting an XCFA instance.
     * Supports .xcfa, .ll, .bc, .c and .i files.
     */
    public static XCFA fromFile(File model) throws IOException {

        if (!model.exists()) throw new FileNotFoundException();

        if (model.getName().endsWith(".xcfa")) {
            try (InputStream is = new FileInputStream(model)) {
                return createXCFA(is);
            }

        } else if (model.getName().endsWith(".ll") || model.getName().endsWith(".bc")) {
            return createXCFA(new LlvmIrProvider(model.getAbsolutePath()));

        } else if (model.getName().endsWith(".c") || model.getName().endsWith(".i")) {
            throw new RuntimeException(".c or .i files are not yet supported.");

        } else {
            String[] split = model.getName().split("\\.");
            if (split.length > 0)
                throw new RuntimeException("File type " + split[split.length - 1] + " not supported.");
            throw new RuntimeException("File does not have an extension.");

        }
    }

    /*
     * Creates an XCFA from the provided InputStream using the XCFA DSL.
     */
    public static XCFA createXCFA(InputStream dsl) throws IOException {
        return XcfaDslManager.createXcfa(dsl);
    }

    /*
     * Creates an XCFA from the provided String using the XCFA DSL
     */
    public static XCFA createXCFA(String dsl) throws IOException {
        return XcfaDslManager.createXcfa(dsl);
    }

    /*
     * Creates an XCFA from the provided SSAProvider using its getter methods.
     */
    public static XCFA createXCFA(SSAProvider ssa) {
        Map<String, VarDecl<?>> globalVarLut = new HashMap<>();
        XCFA.Builder builder = XCFA.builder();

        // Creating global variables
        for (Tuple3<String, String, String> globalVariable : ssa.getGlobalVariables()) {

            VarDecl<?> variable = createVariable(globalVariable.get1(), globalVariable.get2());
            globalVarLut.put(globalVariable.get1(), variable);
            builder.getGlobalVars().put(variable, createConstant(globalVariable.get3()));

        }

        Map<String, XcfaProcedure> procedures = new LinkedHashMap<>();
        Map<XcfaProcess.Builder, String> processBuilders = new HashMap<>();
        List<InstructionHandler> instructionHandlers = new ArrayList<>();

        XcfaProcess.Builder mainProcBuilder = XcfaProcess.builder();
        mainProcBuilder.setName("main");
        processBuilders.put(mainProcBuilder, mainProcBuilder.getName());

        // Creating procedures
        for (Tuple3<String, Optional<String>, List<Tuple2<String, String>>> function : ssa.getFunctions()) {

            XcfaProcedure.Builder procedureBuilder = XcfaProcedure.builder();
            procedureBuilder.setName(function.get1());

            Collection<String> processes = new ArrayList<>();

            instructionHandlers.add(handleProcedure(function, procedureBuilder, ssa, globalVarLut, processes));

            for (String process : processes) {

                XcfaProcess.Builder processBuilder = XcfaProcess.builder();
                processBuilder.setName(process);
                processBuilders.put(processBuilder, function.get1());

            }

            XcfaProcedure procedure = procedureBuilder.build();
            procedures.put(function.get1(), procedure);
        }

        // Letting procedures finish setting up their call statements (by providing them with a list of built procedures)
        for (InstructionHandler instructionHandler : instructionHandlers) {
            instructionHandler.substituteProcedures(procedures);
        }

        // Instantiating procedures, each with a copy of each procedure.
        for (Map.Entry<XcfaProcess.Builder, String> entry : processBuilders.entrySet()) {
            XcfaProcess.Builder processBuilder = entry.getKey();
            String mainProcedureName = entry.getValue();

            for (Map.Entry<String, XcfaProcedure> e : procedures.entrySet()) {
                String procedureName = e.getKey();
                XcfaProcedure procedure = e.getValue();

                XcfaProcedure proc = new XcfaProcedure(procedure);
                processBuilder.addProcedure(proc);
                if (procedureName.equals(mainProcedureName)) processBuilder.setMainProcedure(proc);

            }

            XcfaProcess proc = processBuilder.build();
            builder.addProcess(proc);

            if (processBuilder == mainProcBuilder) builder.setMainProcess(proc);

        }

        return builder.build();
    }
}