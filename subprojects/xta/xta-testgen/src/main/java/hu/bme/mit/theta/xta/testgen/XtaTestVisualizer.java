package hu.bme.mit.theta.xta.testgen;

import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.common.visualization.EdgeAttributes;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.LineStyle;
import hu.bme.mit.theta.common.visualization.NodeAttributes;
import hu.bme.mit.theta.common.visualization.Shape;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XtaTestVisualizer {
	public static void visualizeTests(Set<? extends XtaTest<?, ?>> tests) {
		for (XtaTest<?, ?> test : tests)
			visualizeTest(test);
	}

	public static void visualizeTest(XtaTest<?, ?> test) {
		Graph traceGraph = new Graph(test.getName(), String.format("%s%nTotal time: %f", test.getName(), test.getTotalTime()));

		Map<Integer, XtaProcess.Loc> indexLocMap = new HashMap<>();
		Map<Integer, String> indexIdMap = new HashMap<>();

		ArgTrace<? extends XtaState<?>, ?> trace = test.getTrace();
		for (int i = 0; i < trace.nodes().size(); i++) {
			ArgNode<? extends XtaState<?>, ?> n = trace.nodes().get(i);
			XtaState<?> st = n.getState();

			List<XtaProcess.Loc> locs = st.getLocs();

			for (int j = 0; j < locs.size(); j++) {
				XtaProcess.Loc l = locs.get(j);
				String id = String.format("%s__%d", l.getName(), i);
				String label = String.format("%s%ndelay: %f", id, test.getDelays().get(i));

				NodeAttributes nAttributes = NodeAttributes.builder().label(label).fillColor(Color.WHITE)
						.lineColor(Color.BLACK).lineStyle(LineStyle.NORMAL).peripheries(1).build();

				NodeAttributes invisibleAttributes = NodeAttributes.builder().label(id).fillColor(Color.WHITE)
						.lineColor(Color.BLACK).lineStyle(LineStyle.NORMAL).peripheries(1).invisible(true).build();

				EdgeAttributes eAttributes = EdgeAttributes.builder()
						.color(Color.BLACK).lineStyle(LineStyle.NORMAL).build();

				if (i == 0) {
					NodeAttributes startAttributes = NodeAttributes.builder().fillColor(Color.BLACK)
							.lineColor(Color.BLACK).lineStyle(LineStyle.NORMAL).peripheries(1).shape(Shape.RECTANGLE).build();
					String startId = "start__" + id;
					traceGraph.addNode(startId, startAttributes);
					traceGraph.addNode(id, nAttributes);
					traceGraph.addEdge(startId, id, eAttributes);
				} else {
					String prevId = indexIdMap.get(j);
					if (!indexLocMap.containsValue(l)) {
						traceGraph.addNode(id, nAttributes);
					} else {
						traceGraph.addNode(id, invisibleAttributes);
					}
					traceGraph.addEdge(prevId, id, eAttributes);
				}
				indexLocMap.put(j, l);
				indexIdMap.put(j, id);
			}
		}

		try {
			GraphvizWriter.getInstance().writeFile(traceGraph, traceGraph.getId() + ".png", GraphvizWriter.Format.PNG);
		} catch (Exception  e) {
			e.printStackTrace();
		}
	}
}
