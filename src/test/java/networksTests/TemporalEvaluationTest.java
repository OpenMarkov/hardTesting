package networksTests;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.ParserException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.io.ProbNetInfo;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Node;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.GTablePotential;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.temporalevaluation.tasks.TemporalEvaluation;
import org.openmarkov.inference.variableElimination.tasks.VECEAnalysis;
import org.openmarkov.inference.variableElimination.tasks.VEEvaluation;
import org.openmarkov.io.probmodel.reader.PGMXReader_0_2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TemporalEvaluationTest {

	private ProbNet probNet;

	private EvidenceCase preResolutionEvidence;

	// Delta parameter for Assert.Equals methods
	private final double deltaEquals = Math.pow(10, -4);

	@Before public void setUp() {
		Configurator.setRootLevel(Level.DEBUG);
		// New cost-effectiveness networks
		String networkName = "networks/mid/MID-Chancellor.pgmx";
		InputStream file = getClass().getClassLoader().getResourceAsStream(networkName);
		PGMXReader_0_2 pgmxReader = new PGMXReader_0_2();
		ProbNetInfo probNetInfo = null;
		try {
			probNetInfo = pgmxReader.loadProbNetInfo(networkName, file);
			probNet = probNetInfo.getProbNet();
			preResolutionEvidence = probNetInfo.getEvidence().isEmpty() ? new EvidenceCase() : probNetInfo.getEvidence().get(0);

			List<Node> utilityNodes = probNet.getNodes(NodeType.UTILITY);
			for (Node node : utilityNodes) {
				if (!node.getVariable().getBaseName().equals("Cost lamivudine")) {
					probNet.removeNode(node);
				}
			}

		} catch (ParserException e) {
			e.printStackTrace();
		}
	}

	@Test public void temporalEvolutionTest()
			throws IncompatibleEvidenceException, NotEvaluableNetworkException, UnexpectedInferenceException,
			NodeNotFoundException {
		VECEAnalysis veceAnalysis = new VECEAnalysis(probNet);
		veceAnalysis.setPreResolutionEvidence(preResolutionEvidence);
		veceAnalysis.setDecisionVariable(probNet.getNodes(NodeType.DECISION).get(0).getVariable());
		GTablePotential resultVE = veceAnalysis.getUtility();

		TemporalEvaluation temporalEvaluation = new TemporalEvaluation(probNet);
		temporalEvaluation.setPreResolutionEvidence(preResolutionEvidence);
		temporalEvaluation.setConditioningVariables(Arrays.asList(probNet.getVariable("Therapy type")));

		List<TablePotential> potentialsPerSlice = temporalEvaluation.getUtilityPotentialsPerSlice();

		for (TablePotential potential : potentialsPerSlice) {
			LogManager.getLogger().debug(potential.toString());
		}


	}

}
