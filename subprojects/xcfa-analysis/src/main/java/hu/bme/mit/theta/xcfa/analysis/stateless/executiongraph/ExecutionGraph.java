package hu.bme.mit.theta.xcfa.analysis.stateless.executiongraph;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutablePartitionedValuation;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.booltype.BoolLitExpr;
import hu.bme.mit.theta.mcm.MCM;
import hu.bme.mit.theta.mcm.Result;
import hu.bme.mit.theta.mcm.graph.constraint.Constraint;
import hu.bme.mit.theta.xcfa.XCFA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class ExecutionGraph implements Runnable{
    private static final XcfaStmtExecutionVisitor xcfaStmtExecutionVisitor;

    private ThreadPoolExecutor threadPool;

    private final MCM mcm;                                                    //deep
    private final XCFA xcfa;                                                  //shallow
    private final Set<Write> initialWrites;                                   //shallow
    private final Map<XCFA.Process, MemoryAccess> lastNode;                   //deep
    private final Map<XCFA.Process, Map<VarDecl<?>, Read>> lastRead;          //deep
    private final Map<VarDecl<?>, List<Read>> revisitableReads;               //deep
    private final Map<VarDecl<?>, List<Write>> writes;                        //deep
    private final Map<Read, Tuple2<Write, Tuple2<MemoryAccess, String>>> fr;  //deep
    private final Map<VarDecl<?>, List<Write>> mo;                            //deep
    private final Map<MemoryAccess, Set<Tuple2<MemoryAccess, String>>> edges; //deep

    private final Map<XCFA.Process, List<StackFrame>> stackFrames;            //deep
    private final MutablePartitionedValuation mutablePartitionedValuation;    //deep
    private XCFA.Process currentlyAtomic;                                     //shallow

    private final Map<XCFA.Process, Integer> partitions;                      //shallow
    
    private final List<Integer> path;                                         //deep
    private int step;                                                   //shallow


    //CONSTRUCTORS

    private ExecutionGraph(XCFA xcfa, MCM mcm) {
        this.mcm = mcm;
        initialWrites = new HashSet<>();
        lastNode = new HashMap<>();
        lastRead = new HashMap<>();
        revisitableReads = new HashMap<>();
        writes = new HashMap<>();
        fr = new HashMap<>();
        stackFrames = new HashMap<>();
        currentlyAtomic = null;
        mutablePartitionedValuation = new MutablePartitionedValuation();
        partitions = new HashMap<>();
        edges = new HashMap<>();
        this.xcfa = xcfa;
        this.path = new ArrayList<>();

        mo = new HashMap<>();
        xcfa.getGlobalVars().forEach(varDecl -> mo.put(varDecl, new ArrayList<>()));

        xcfa.getProcesses().forEach(process -> {
            stackFrames.put(process, new ArrayList<>());
            partitions.put(process, mutablePartitionedValuation.createPartition());
            lastRead.put(process, new HashMap<>());
        });

        xcfa.getGlobalVars().forEach(varDecl -> {
            revisitableReads.put(varDecl, new ArrayList<>());
            writes.put(varDecl, new ArrayList<>());
            LitExpr<?> litExpr;
            if((litExpr = xcfa.getInitValue(varDecl)) != null) {
                addInititalWrite(varDecl, litExpr);
            }
        });
    }

    private ExecutionGraph(
            ThreadPoolExecutor threadPool,
            XCFA xcfa,
            Set<Write> initialWrites,
            Map<XCFA.Process, MemoryAccess> lastNode,
            Map<XCFA.Process, Map<VarDecl<?>, Read>> lastRead,
            Map<VarDecl<?>, List<Read>> revisitableReads,
            Map<VarDecl<?>, List<Write>> writes,
            Map<MemoryAccess, Set<Tuple2<MemoryAccess, String>>> edges,
            Map<Read, Tuple2<Write, Tuple2<MemoryAccess, String>>> fr,
            Map<VarDecl<?>, List<Write>> mo,
            Map<XCFA.Process, List<StackFrame>> stackFrames,
            MCM mcm,
            XCFA.Process currentlyAtomic,
            MutablePartitionedValuation mutablePartitionedValuation,
            Map<XCFA.Process, Integer> partitions,
            List<Integer> path){
        this.threadPool = threadPool;
        this.xcfa = xcfa;
        this.initialWrites = initialWrites;
        this.lastNode = new HashMap<>(lastNode);
        this.fr = new HashMap<>(fr);
        this.mcm = mcm.duplicate();
        this.mo = new HashMap<>();
        mo.forEach((varDecl, writes1) -> this.mo.put(varDecl, new ArrayList<>(writes1)));
        this.path = path;
        this.lastRead = new HashMap<>();
        lastRead.forEach((process, varDeclReadMap) -> this.lastRead.put(process, new HashMap<>(varDeclReadMap)));
        this.revisitableReads = new HashMap<>();
        revisitableReads.forEach((varDecl, reads) -> this.revisitableReads.put(varDecl, new ArrayList<>(reads)));
        this.writes = new HashMap<>();
        writes.forEach((varDecl, writes1) -> this.writes.put(varDecl, new ArrayList<>(writes1)));
        this.edges = new HashMap<>();
        edges.forEach((memoryAccess, tuples) -> this.edges.put(memoryAccess, new HashSet<>(tuples)));
        this.stackFrames = new HashMap<>();
        stackFrames.forEach((process, stackFrames1) -> this.stackFrames.put(process, new ArrayList<>(stackFrames1)));
        this.stackFrames.forEach((process, stackFrameList) -> {
            int lastId = stackFrameList.size() - 1;
            if(lastId != -1) {
                StackFrame stackFrame;
                if (!(stackFrame = stackFrameList.get(lastId)).isLastStmt()) {
                    stackFrameList.remove(lastId);
                    stackFrameList.add(stackFrame.duplicate());
                }
            }
        });
        this.currentlyAtomic = currentlyAtomic;
        this.mutablePartitionedValuation = MutablePartitionedValuation.copyOf(mutablePartitionedValuation);
        this.partitions = partitions;
    }

    // STATIC METHODS

    static {
        xcfaStmtExecutionVisitor = new XcfaStmtExecutionVisitor();
    }

    /*
     * Create a new ExecutionGraph and return it
     */
    public static ExecutionGraph create(XCFA xcfa, MCM mcm) {
        return new ExecutionGraph(xcfa, mcm);
    }





    // PUBLIC METHODS
    private static AtomicInteger cnt = new AtomicInteger(0);

    public void execute(int threads) {
        threadPool = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        threadPool.execute(this);
        try {
            if(!threadPool.awaitTermination(600, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * Run the algorithm on the current ExecutionGraph
     */
    @Override
    public void run() {
        if(threadPool.getCompletedTaskCount() % 1000 == 0) {
            System.out.println("Active: " + threadPool.getActiveCount() + ", Queue: " + threadPool.getQueue().size() + ", Finished: "+ threadPool.getCompletedTaskCount());
        }
        cnt.incrementAndGet();
        step = 0;
        while(executeNextStmt()) {
            step++;
        }
        try {
            printGraph(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        testQueue();
    }


    // PACKAGE-PRIVATE METHODS

    /*
     * Add a read node
     */
    void addRead(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        Read read = new Read(
                global,
                local,
                mutablePartitionedValuation.getValuation(getPartitionId(proc)),
                stackFrames.get(proc),
                lastRead.get(proc).get(global),
                proc,
                currentlyAtomic == proc,
                lastNode.get(proc),
                true); // TODO
        lastRead.get(proc).put(global, read);
        addNode(proc, read);

        int size = writes.get(global).size();
        for(int i = 0; i < size; ++i) {
            Write write = writes.get(global).get(i);
            Tuple2<MemoryAccess, String> edge = Tuple2.of(read, "rf");
            if(i < size - 1) {
                ExecutionGraph executionGraph = duplicate(i, step);
                executionGraph.edges.get(write).add(edge);
                executionGraph.mcm.checkMk(write, read, "rf", write.isFinal() && read.isFinal());
                executionGraph.mutablePartitionedValuation.put(getPartitionId(proc),global,write.getValue());
                executionGraph.fr.put(read, Tuple2.of(write, edge));
                threadPool.execute(executionGraph);
            }
            else {
                edges.get(write).add(Tuple2.of(read, "rf"));
                mcm.checkMk(write, read, "rf", false);
                mutablePartitionedValuation.put(getPartitionId(proc),global,write.getValue());
                fr.put(read, Tuple2.of(write, edge));
                revisitableReads.get(global).add(read);
            }
        }

    }

    /*
     * Add a fence node
     */
    void addFence(XCFA.Process proc, String type) {
        Fence fence = new Fence(null, proc, lastNode.get(proc), type, true); //TODO
        addNode(proc, fence);
    }

    /*
     * Add a write node
     */
    void addWrite(XCFA.Process proc, VarDecl<?> local, VarDecl<?> global) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") Write write = new Write(global, mutablePartitionedValuation.eval(local).get(), proc, lastNode.get(proc), true); //TODO
        addNode(proc, write);
        this.writes.get(global).add(write);
        List<List<Read>> revisitSets = getRevisitSets(global);
        int childCnt = 0;
        int size = mo.get(global).size();
        for(int j = -1; j < size; ++j) {
            for(int i = 0; i < revisitSets.size(); ++i) {
                List<Read> reads = revisitSets.get(i);
                ExecutionGraph executionGraph;
                if(i < revisitSets.size() - 1 || j < size - 1) {
                    executionGraph = this.duplicate(childCnt++, step);
                }
                else {
                    executionGraph = this;
                }

                if(j == -1) {
                    if(size > 0) {
                        executionGraph.edges.get(write).add(Tuple2.of(mo.get(global).get(j+1), "mo"));
                        executionGraph.mcm.checkMk(write, mo.get(global).get(j+1), "mo", false);
                    }
                }
                else if(j < size-1) {
                    executionGraph.edges.get(mo.get(global).get(j)).remove(Tuple2.of(mo.get(global).get(j+1), "mo"));
                    executionGraph.mcm.checkRm(mo.get(global).get(j), mo.get(global).get(j+1), "mo");
                    executionGraph.edges.get(write).add(Tuple2.of(mo.get(global).get(j+1), "mo"));
                    executionGraph.mcm.checkMk(write, mo.get(global).get(j+1), "mo", false);
                    executionGraph.edges.get(mo.get(global).get(j)).add(Tuple2.of(write, "mo"));
                    executionGraph.mcm.checkMk(mo.get(global).get(j), write, "mo", false);
                }
                else {
                    executionGraph.edges.get(mo.get(global).get(j)).add(Tuple2.of(write, "mo"));
                    executionGraph.mcm.checkMk(mo.get(global).get(j), write, "mo", false);
                }
                executionGraph.mo.get(global).add(j+1, write);

                for(Read read : reads) {
                    Tuple2<MemoryAccess, String> edge = Tuple2.of(read, "rf");
                    executionGraph.revisitRead(read);
                    executionGraph.edges.get(write).add(edge);
                    executionGraph.mcm.checkMk(write, read, "rf", write.isFinal() && read.isFinal());
                    executionGraph.fr.put(read, Tuple2.of(write, edge));
                    executionGraph.mutablePartitionedValuation.put(getPartitionId(proc),global,write.getValue());
                }

                if(i < revisitSets.size() - 1 || j < size - 1) {
                    threadPool.execute(executionGraph);
                }
            }

        }

    }

    /*
     * Add an initial write node
     */
    void addInititalWrite(VarDecl<?> global, LitExpr<?> value) {
        Write write = new Write(global, value, null, null, true);
        edges.put(write, new HashSet<>());
        initialWrites.add(write);
        if(!writes.containsKey(global)) {
            writes.put(global, new ArrayList<>());
        }
        writes.get(global).add(write);
        List<Write> writes = mo.get(global);
        if(writes.isEmpty()) {
            writes.add(write);
        } else {
            throw new UnsupportedOperationException("Trying to create two initial writes for the same variable!");
        }
    }

    void setCurrentlyAtomic(XCFA.Process currentlyAtomic) {
        this.currentlyAtomic = currentlyAtomic;
    }


    int getPartitionId(XCFA.Process process) {
        return partitions.get(process);
    }





    //PRIVATE METHODS

    /*
     * Returns a duplicate of the current ExecutionGraph
     */
    private ExecutionGraph duplicate(int i, int step) {
        List<Integer> newPath = new ArrayList<>(path);
        newPath.add(step);
        newPath.add(i);
        return new ExecutionGraph(threadPool, xcfa, initialWrites, lastNode, lastRead, revisitableReads, writes, edges, fr, mo, stackFrames, mcm, currentlyAtomic, mutablePartitionedValuation, partitions, newPath);
    }

    /*
     * Returns the current revisit (sub)sets of variable 'global'
     */
    private List<List<Read>> getRevisitSets(VarDecl<?> global) {
        List<List<Read>> ret = new ArrayList<>();
        if(revisitableReads.get(global) == null) return ret;
        for(int i = 0; i < (1<<revisitableReads.get(global).size()); ++i) {
            List<Read> list = new ArrayList<>();
            for(int j = 0; j < revisitableReads.get(global).size(); ++j) {
                if((i & (1<<j)) != 0) {
                    list.add(revisitableReads.get(global).get(j));
                }
            }
            ret.add(list);
        }
        return ret.stream().filter(reads -> {
            Set<XCFA.Process> processes = new HashSet<>();
            for(Read r : reads) {
                processes.add(r.getProcess());
            }
            return processes.size() == reads.size();
        }).collect(Collectors.toList());
    }

    private Collection<XCFA.Process> getProcesses() {
        if(currentlyAtomic != null) return Collections.singleton(currentlyAtomic);
        else {
            return xcfa.getProcesses();
        }
    }

    /*
     * Executes the next statement to execute
     */
    private boolean executeNextStmt() {
        for(XCFA.Process process : getProcesses()) {
            StackFrame stackFrame;
            if(stackFrames.get(process).size() == 0) {
                if (handleNewEdge(process, process.getMainProcedure().getInitLoc())) {
                    return true;
                }
            }
            else if((stackFrame = stackFrames.get(process).get(stackFrames.get(process).size()-1)).isLastStmt()) {
                if (handleNewEdge(process, stackFrame.getEdge().getTarget())) {
                    return true;
                }
            }
            else {
                if (handleCurrentEdge(process, stackFrame)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleNewEdge(XCFA.Process process, XCFA.Process.Procedure.Location newSource) {
        for(XCFA.Process.Procedure.Edge edge : newSource.getOutgoingEdges()) {
            boolean canExecute = true;
            for(Stmt stmt : edge.getStmts()) {
                if (stmt instanceof AssumeStmt) {
                    canExecute = ((BoolLitExpr) ((AssumeStmt) stmt).getCond().eval(mutablePartitionedValuation)).getValue();
                }
            }
            if(canExecute) {
                for(Stmt stmt : edge.getStmts()) {
                    List<StackFrame> stackFrameList = stackFrames.get(process);
                    StackFrame stackFrame;
                    if(stackFrameList.size() > 0 && (stackFrame = stackFrameList.get(stackFrameList.size() - 1)).isLastStmt()) {
                        stackFrameList.remove(stackFrame);
                    }
                    stackFrameList.add(new StackFrame(edge, stmt));
                    stmt.accept(xcfaStmtExecutionVisitor, Tuple3.of(mutablePartitionedValuation, process, this));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleCurrentEdge(XCFA.Process process, StackFrame stackFrame) {
        Stmt nextStmt = null;
        boolean found = false;
        for (Stmt stmt : stackFrame.getEdge().getStmts()) {
            if (stmt == stackFrame.getStmt()){
                found = true;
            }
            else if(found) {
                nextStmt = stmt;
                break;
            }
        }
        if(nextStmt != null) {
            stackFrame.setStmt(nextStmt);
            nextStmt.accept(xcfaStmtExecutionVisitor, Tuple3.of(mutablePartitionedValuation, process, this));
            return true;
        }
        else {
            stackFrame.setLastStmt();
            return handleNewEdge(process, stackFrame.getEdge().getTarget());
        }
    }


    private void addNode(XCFA.Process proc, MemoryAccess memoryAccess) {
        edges.put(memoryAccess, new HashSet<>());
        if(lastNode.get(proc) != null) {
            edges.get(lastNode.get(proc)).add(Tuple2.of(memoryAccess, "po"));
            mcm.checkMk(lastNode.get(proc), memoryAccess, "po", lastNode.get(proc).isFinal() && memoryAccess.isFinal());
        }
        else {
            initialWrites.forEach(write -> {
                edges.get(write).add(Tuple2.of(memoryAccess, "po"));
                mcm.checkMk(write, memoryAccess, "po", lastNode.get(proc).isFinal() && memoryAccess.isFinal());
            });
        }
        lastNode.put(proc, memoryAccess);
    }


    private void revisitRead(Read read) {
        for(Read r : read.getPrecedingReads()) {
            revisitableReads.get(r.getGlobalVariable()).remove(r);
        }
        invalidateFuture(read);
        lastNode.put(read.getProcess(), read);
    }

    private void invalidateFuture(Read read) {
        Map<XCFA.Process, Boolean> atomic = new HashMap<>();
        invalidateFuture(read, atomic, true);

        boolean foundOne = false;
        for (Map.Entry<XCFA.Process, Boolean> entry : atomic.entrySet()) {
            XCFA.Process process = entry.getKey();
            Boolean atomicity = entry.getValue();
            if (atomicity) {
                checkState(!foundOne, "Multiple processes cannot be concurrently atomic!");
                foundOne = true;
                currentlyAtomic = process;
            }
        }
    }

    private void invalidateFuture(MemoryAccess memoryAccess, Map<XCFA.Process, Boolean> atomic, boolean first) {
        if(memoryAccess instanceof Read) {
            for (Tuple2<MemoryAccess, String> objects : edges.get(fr.get(memoryAccess).get1())) {
                mcm.checkRm(fr.get(memoryAccess).get1(), objects.get1(), objects.get2());
            }
            edges.get(fr.get(memoryAccess).get1()).remove(fr.get(memoryAccess).get2());
            fr.remove(memoryAccess);
            revisitableReads.get(memoryAccess.getGlobalVariable()).remove(memoryAccess);
        }
        else if(memoryAccess instanceof Write) {
            int i = mo.get(memoryAccess.getGlobalVariable()).indexOf(memoryAccess);
            if(i > 0) {
                mcm.checkRm(mo.get(memoryAccess.getGlobalVariable()).get(i - 1), memoryAccess, "mo");
            }
            if(i < mo.get(memoryAccess.getGlobalVariable()).size()-1) {
                mcm.checkRm(memoryAccess, mo.get(memoryAccess.getGlobalVariable()).get(i + 1), "mo");
                if(i > 0) {
                    mcm.checkMk(mo.get(memoryAccess.getGlobalVariable()).get(i - 1), mo.get(memoryAccess.getGlobalVariable()).get(i + 1), "mo", false);
                }
            }
            mo.get(memoryAccess.getGlobalVariable()).remove(memoryAccess);
        }
        for(Tuple2<MemoryAccess, String> edge : edges.get(memoryAccess)) {
            invalidateFuture(edge.get1(), atomic, false);
        }
        atomic.put(memoryAccess.getProcess(), memoryAccess.revert(stackFrames, lastNode, mutablePartitionedValuation, getPartitionId(memoryAccess.getProcess())));
        if(first) {
            edges.put(memoryAccess, new HashSet<>());
        }
        else {
            edges.remove(memoryAccess);
        }
    }


    private synchronized void testQueue() {
        if(threadPool.getQueue().size() == 0 && threadPool.getActiveCount() == 1) {
            threadPool.shutdown();
            System.out.println("Traces: " + cnt.get());
        }
    }

    /*
     * Prints the graph as a graphviz cluster
     */
    private void printGraph(boolean isFinal) throws IOException {
        File outFile;
        if(!isFinal) {
            StringBuilder path = new StringBuilder("out").append(File.separator).append("steps").append(File.separator);
            for (Integer integer : this.path) {
                path.append(integer).append(File.separator);
            }
            outFile = new File(path.append(step).append("graph.dot").toString());
        }
        else {
            StringBuilder path = new StringBuilder("out").append(File.separator).append("final").append(File.separator);
            for (Integer integer : this.path) {
                path.append(integer);
            }
            outFile = new File(path.append("graph.dot").toString());
        }
        if (outFile.getParentFile().exists() || outFile.getParentFile().mkdirs()) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile))) {
                bufferedWriter.write("digraph G {");
                bufferedWriter.newLine();
                for (Write initialWrite : initialWrites) {
                    bufferedWriter.write(initialWrite.toString());
                    bufferedWriter.newLine();
                }
                for (XCFA.Process process : stackFrames.keySet()) {
                    bufferedWriter.write("subgraph cluster_" + process.getName() + "{");
                    bufferedWriter.newLine();
                    for (MemoryAccess memoryAccess : edges.keySet()) {
                        if (memoryAccess.getProcess() == process) {
                            bufferedWriter.write(memoryAccess.toString());
                            if (memoryAccess instanceof Read && revisitableReads.get(memoryAccess.getGlobalVariable()).contains(memoryAccess)) {
                                bufferedWriter.write(" [style=filled]");
                            }
                            bufferedWriter.newLine();
                        }
                    }
                    bufferedWriter.write("}");
                    bufferedWriter.newLine();
                }
                for (Map.Entry<MemoryAccess, Set<Tuple2<MemoryAccess, String>>> entry : edges.entrySet()) {
                    MemoryAccess memoryAccess = entry.getKey();
                    Set<Tuple2<MemoryAccess, String>> tuple2s = entry.getValue();
                    for (Tuple2<MemoryAccess, String> tuple2 : tuple2s) {
                        bufferedWriter.write(memoryAccess + " -> " + tuple2.get1());
                        switch (tuple2.get2()) {
                            case "po":
                                break;
                            case "rf":
                                bufferedWriter.write(" [constraint=false,color=green,fontcolor=green,style=dashed,label=rf]");
                                break;
                            case "mo":
                                bufferedWriter.write(" [constraint=false,color=purple,fontcolor=purple,style=dashed,label=mo]");
                                break;
                            default:
                                bufferedWriter.write(" [constraint=false,color=grey,style=dashed,label=" + tuple2.get2() + "]");
                                break;
                        }
                        bufferedWriter.newLine();
                    }
                }
                bufferedWriter.write("fontcolor=red");
                bufferedWriter.newLine();
                bufferedWriter.write("label=\"");
                for (Map.Entry<Constraint, Result> entry : mcm.getConstraints().entrySet()) {
                    Constraint constraint = entry.getKey();
                    Result result = entry.getValue();
                    if (result.getResult() != Result.ResultType.OK) {
                        bufferedWriter.write(constraint.getName() + System.lineSeparator());
                    }
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
                bufferedWriter.write("}");
            }
        }
    }

}
