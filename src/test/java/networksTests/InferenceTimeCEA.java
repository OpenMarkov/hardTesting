package networksTests;

import cern.colt.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openmarkov.core.io.ProbNetInfo;
import org.openmarkov.core.model.network.CEP;
import org.openmarkov.core.model.network.Criterion;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.inference.algorithm.decompositionIntoSymmetricDANs.ceanalysis.DANDecisionTreeCEA;
import org.openmarkov.inference.algorithm.decompositionIntoSymmetricDANs.ceanalysis.DANDecompositionIntoSymmetricDANsCEA;
import org.openmarkov.inference.algorithm.decompositionIntoSymmetricDANs.evaluation.DANDecisionTreeEvaluation;
import org.openmarkov.inference.algorithm.decompositionIntoSymmetricDANs.evaluation.DANDecompositionIntoSymmetricDANsEvaluation;
import org.openmarkov.integrationTests.IntegrationTest;
import org.openmarkov.io.probmodel.reader.PGMXReader_0_2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class InferenceTimeCEA {
    
    private static final String path = "networks/dan/";
    // Delta parameter for Equals methods
    private final double deltaEquals = Math.pow(10, -4);
    private final double lambda = 30000;
    private List<AnalysisResult> results = new ArrayList<>();
    
    static Stream<ProbNetContents> networksToTest() throws Exception {
        PGMXReader_0_2 pgmxReader = new PGMXReader_0_2();
        var networkNames = List.of(
                // New cost-effectiveness networks
                "DAN-CE-2-test-problem.pgmx",
                "DAN-CE-3-test-problem.pgmx",
                "DAN-CE-4-test-problem.pgmx",
                "DAN-CE-5-test-problem.pgmx",
                "DAN-CE-6-test-problem.pgmx",
                "DAN-CE-7-test-problem.pgmx",
                "DAN-CE-8-test-problem.pgmx",
                // Old unicriterion n-test DANs
                "DAN-0-test-problem.pgmx",
                "DAN-1-test-problem.pgmx",
                "DAN-2-test-problem.pgmx",
                "DAN-3-test-problem.pgmx",
                "DAN-4-test-problem.pgmx",
                "DAN-5-test-problem.pgmx",
                "DAN-6-test-problem.pgmx",
                "DAN-7-test-problem.pgmx",
                "DAN-mediastinet-ce.pgmx"
        );
        var networks = new ArrayList<ProbNetContents>(networkNames.size());
        for (String networkName : networkNames) {
            InputStream file = new IntegrationTest().getClass()
                                                    .getClassLoader()
                                                    .getResourceAsStream(path + networkName);
            ProbNetInfo probNetInfo = pgmxReader.loadProbNetInfo(networkName, file);
            ProbNet probNet = probNetInfo.getProbNet();
            networks.add(new ProbNetContents(networkName, probNet, probNetInfo));
        }
        return networks.stream();
    }
    
    @ParameterizedTest
    @MethodSource("networksToTest")
    public void dansTEST(ProbNetContents networkContents) throws Exception {
        ProbNetInfo probNetInfo = networkContents.probNetInfo;
        ProbNet probNet = networkContents.probNet;
        
        EvidenceCase evidenceCase = probNetInfo.getEvidence().isEmpty() ?
                new EvidenceCase() :
                probNetInfo.getEvidence().get(0);
        
        for (Criterion criterion : probNet.getDecisionCriteria()) {
            LogManager.getLogger()
                      .debug(criterion.getCriterionName() + " scale = (x " + criterion.getUnicriterizationScale()
                                     + ")");
        }
        
        // UNICRITERION ANALYSIS
        LogManager.getLogger().debug("DSD for " + probNet.getName());
        long startTime = System.nanoTime();
        DANDecompositionIntoSymmetricDANsEvaluation evaluationDSD = new DANDecompositionIntoSymmetricDANsEvaluation(
                probNet, evidenceCase);
        TablePotential utilityDSD = evaluationDSD.getUtility();
        long endTime = System.nanoTime();
        
        long dsdUNIEvaluationTime = endTime - startTime;
        
        LogManager.getLogger().debug("DT for " + probNet.getName());
        startTime = System.nanoTime();
        DANDecisionTreeEvaluation evaluationDT = new DANDecisionTreeEvaluation(probNet, evidenceCase);
        TablePotential utilityDT = evaluationDT.getUtility();
        endTime = System.nanoTime();
        
        long dtUNIEvaluationTime = endTime - startTime;
        evaluationDT = null;
        
        // Check that the result of both unicriterion algorithms are the same
        assertArrayEquals(utilityDSD.getValues(), utilityDT.getValues(), deltaEquals);
        
        // COST-EFFECTIVENESS ANALYSIS
        LogManager.getLogger().debug("CEA_DSD with lambda = " + lambda);
        startTime = System.nanoTime();
        DANDecompositionIntoSymmetricDANsCEA evaluationCEADSD = new DANDecompositionIntoSymmetricDANsCEA(
                probNet, evidenceCase);
        CEP cepDSD = evaluationCEADSD.getCEP();
        endTime = System.nanoTime();
        
        long dsdCEEvaluationTime = endTime - startTime;
        evaluationCEADSD = null;
        
        LogManager.getLogger().debug("CEA_DT with lambda = " + lambda);
        startTime = System.nanoTime();
        DANDecisionTreeCEA evaluationCEADT = new DANDecisionTreeCEA(probNet);
        CEP cepDT = evaluationCEADT.getCEP();
        endTime = System.nanoTime();
        
        long dtCEEvaluationTime = endTime - startTime;
        
        AnalysisResult result = new AnalysisResult(
                dsdCEEvaluationTime,
                dtCEEvaluationTime,
                dsdUNIEvaluationTime,
                dtUNIEvaluationTime,
                probNet.getName(),
                cepDSD,
                cepDT,
                utilityDSD,
                utilityDT
        );
        evaluationCEADT = null;
        
        // Check that the result of both CE algorithms are the same
        double[] cepDSDThresholds = cepDSD.getThresholds();
        double[] cepDTThresholds = cepDT.getThresholds();
        double[] cepDSDCosts = cepDSD.getCosts();
        double[] cepDTCosts = cepDT.getCosts();
        double[] cepDSDEffectivities = cepDSD.getEffectivities();
        double[] cepDTEffectivities = cepDT.getEffectivities();
        double ceAlgorithmsResults = cepDSD.getEffectiveness(lambda) * lambda - cepDSD.getCost(lambda);
        String rawResult = "-" + probNet.getName() + "-" + System.lineSeparator() +
                "cepDSDThresholds: " + Arrays.toString(cepDSDThresholds) + System.lineSeparator() +
                "cepDTThresholds: " + Arrays.toString(cepDTThresholds) + System.lineSeparator() +
                "cepDSDCosts: " + Arrays.toString(cepDSDCosts) + System.lineSeparator() +
                "cepDTCosts: " + Arrays.toString(cepDTCosts) + System.lineSeparator() +
                "cepDSDEffectivities: " + Arrays.toString(cepDSDEffectivities) + System.lineSeparator() +
                "cepDTEffectivities: " + Arrays.toString(cepDTEffectivities) + System.lineSeparator() +
                "expected result: " + utilityDSD.getValues()[0] + System.lineSeparator() +
                "actual result:" + ceAlgorithmsResults;
        try {
            Files.write(Path.of("DAN Test - Raw Result for " + probNet.getName() + ".txt"), rawResult.getBytes());
        } catch (IOException ignored) {
        }
        
        assertArrayEquals(cepDSDThresholds, cepDTThresholds, deltaEquals);
        assertArrayEquals(cepDSDCosts, cepDTCosts, deltaEquals);
        assertArrayEquals(cepDSDEffectivities, cepDTEffectivities, deltaEquals);
        
        // Check that the result obtained for CE algorithms (lambda=30,000) and Unicriterion algorithms are the same
        assertEquals(utilityDSD.getValues()[0], ceAlgorithmsResults, deltaEquals);
        results.add(result);
        
    }
    
    @ParameterizedTest
    @MethodSource("networksToTest")
    public void checkCEPThresholdsWithUnicreterionAnalysis(ProbNetContents networkContents) throws Exception {
        double precision = Math.pow(10, 4);
        ProbNetInfo probNetInfo = networkContents.probNetInfo;
        ProbNet probNet = networkContents.probNet;
        String networkName = probNet.getName();
        EvidenceCase evidenceCase = probNetInfo.getEvidence().isEmpty() ?
                new EvidenceCase() :
                probNetInfo.getEvidence().get(0);
        
        /**
         * If multicriteria
         */
        boolean isMulticriteria = false;
        if (probNet.getDecisionCriteria().size() > 1) {
            isMulticriteria = true;
            for (Criterion criterion : probNet.getDecisionCriteria()) {
                if (criterion.getCECriterion() == Criterion.CECriterion.Effectiveness) {
                    // Set effectiveness scale to lambda
                    probNet.getDecisionCriteria().get(1).setUnicriterizationScale(lambda);
                }
            }
        }
        
        TablePotential utilityDSD;
        TablePotential utilityDT;
        
        for (Criterion criterion : probNet.getDecisionCriteria()) {
            LogManager.getLogger()
                      .debug(criterion.getCriterionName() + " scale = (x " + criterion.getUnicriterizationScale()
                                     + ")");
        }
        
        // COST-EFFECTIVENESS ANALYSIS
        LogManager.getLogger().debug("CEA_DSD for " + probNet.getName());
        DANDecompositionIntoSymmetricDANsCEA evaluationCEADSD = new DANDecompositionIntoSymmetricDANsCEA(
                probNet, evidenceCase);
        CEP cepDSD = evaluationCEADSD.getCEP();
        evaluationCEADSD = null;
        
        LogManager.getLogger().debug("CEA_DT for " + probNet.getName());
        DANDecisionTreeCEA evaluationCEADT = new DANDecisionTreeCEA(probNet);
        CEP cepDT = evaluationCEADT.getCEP();
        evaluationCEADT = null;
        
        // Check that the result of both CE algorithms are the same
        try {
            assertArrayEquals(cepDSD.getThresholds(), cepDT.getThresholds(), deltaEquals);
            assertArrayEquals(cepDSD.getCosts(), cepDT.getCosts(), deltaEquals);
            assertArrayEquals(cepDSD.getEffectivities(), cepDT.getEffectivities(), deltaEquals);
        } catch (AssertionError error) {
            LogManager.getLogger()
                      .error("CEPs are different, analyzing with unicriterion analysis. " + error.getMessage());
            
            List<Double> allThresholds = new ArrayList<>();
            LogManager.getLogger().debug("DSD thresholds");
            String loggerThresholds = "";
            for (double threshold : cepDSD.getThresholds()) {
                allThresholds.add(threshold);
                loggerThresholds += threshold + "\t";
            }
            LogManager.getLogger().debug(loggerThresholds);
            
            LogManager.getLogger().debug("DT thresholds");
            loggerThresholds = "";
            for (double threshold : cepDT.getThresholds()) {
                allThresholds.add(threshold);
                loggerThresholds += threshold + "\t";
            }
            LogManager.getLogger().debug(loggerThresholds);
            
            allThresholds = new ArrayList<>(new HashSet<>(allThresholds));
            Collections.sort(allThresholds);
            
            LogManager.getLogger()
                      .debug("Threshold \t DSD Unicriterion \t DT Unicriterion \t DSD Cost \t DT Cost \t DSD Effectiveness \t DT Effectiveness");
            for (double lambda : allThresholds) {
//                        // Set effectiveness scale to lambda - 1
//                        probNet.getDecisionCriteria().get(1).setUnicriterizationScale(lambda);
                
                // UNICRITERION ANALYSIS
                DANDecompositionIntoSymmetricDANsEvaluation evaluationDSD = new DANDecompositionIntoSymmetricDANsEvaluation(
                        probNet, evidenceCase);
                utilityDSD = evaluationDSD.getUtility();
                evaluationDSD = null;
                
                DANDecisionTreeEvaluation evaluationDT = new DANDecisionTreeEvaluation(probNet, evidenceCase);
                utilityDT = evaluationDT.getUtility();
                evaluationDT = null;
                
                LogManager.getLogger().debug(
                        lambda + "\t"
                                + utilityDSD + "\t"
                                + utilityDT + "\t"
                                + cepDSD.getCost(lambda) + "\t"
                                + cepDT.getCost(lambda) + "\t"
                                + cepDSD.getEffectiveness(lambda) + "\t"
                                + cepDT.getEffectiveness(lambda));
//                        LogManager.getLogger().debug(utilityDSD);
//                        LogManager.getLogger().debug(utilityDT);
//                        LogManager.getLogger().debug(cepDSD.getCost(lambda));
//                        LogManager.getLogger().debug(cepDT.getCost(lambda));
//                        LogManager.getLogger().debug(cepDSD.getEffectiveness(lambda));
//                        LogManager.getLogger().debug(cepDT.getEffectiveness(lambda));
                
                try {
                    // Check that the result of both unicriterion algorithms are the same
                    assertArrayEquals(utilityDSD.getValues(), utilityDT.getValues(), deltaEquals);
                } catch (AssertionError err) {
                    LogManager.getLogger()
                              .error("Unicriterion utilities are different for lambda = " + lambda + ". " + err
                                      .getMessage());
                }
                
                try {
                    // Check that the result obtained for CE algorithms and Unicriterion algorithms are the same
                    assertEquals(utilityDSD.getValues()[0], cepDSD.getEffectiveness(lambda) * lambda - cepDSD.getCost(lambda), deltaEquals);
                } catch (AssertionError err) {
                    LogManager.getLogger()
                              .error("DSD CEP are not equal to unicriterion case for lambda = " + lambda + ". " + err.getMessage());
                }
                
                try {
                    assertEquals(utilityDSD.getValues()[0], cepDT.getEffectiveness(lambda) * lambda - cepDT.getCost(lambda), deltaEquals);
                } catch (AssertionError err) {
                    LogManager.getLogger()
                              .error("DT CEP are not equal to unicriterion case for lambda = " + lambda + ". " + err.getMessage());
                }
            }
        }
    }
    
    // TODO: This code no longer compiles
    /*
    @Test public void measureInferenceTime() throws Exception {
        long startTime, endTime;
        FileWriter fileWriter;
        PrintWriter printWriter = null;
        Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("/home/manuel/idea-workspace/org.openmarkov.full/src/test/resources/networks/IDCEAnTherapies/results.txt"), "utf-8"));
        writer.write("Inference times\n");
        writer.write("---------------\n");
        
        startTime = System.nanoTime();
        CEAnalysis decompositionAlgorithmArticleCEA = null;
        decompositionAlgorithmArticleCEA = new CEADecompositionIntoSymmetricDANsEvaluation(
                probNets[0], null, preResolutionEvidence);
        
        CEP cep = (CEP) decompositionAlgorithmArticleCEA.getUtility().elementTable.get(0);
        endTime = System.nanoTime();
        
        for (int i = 0; i < probNets.length; i++) {
            VECEAnalysis veEvaluation;
            startTime = System.nanoTime();
            decompositionAlgorithmArticleCEA = new CEADecompositionIntoSymmetricDANsEvaluation(
                    probNets[i], null, preResolutionEvidence);
            cep = (CEP) decompositionAlgorithmArticleCEA.getUtility().elementTable.get(0);
            endTime = System.nanoTime();
            int numIterations = 1;
            if (endTime - startTime < 100000000L) {
                numIterations = 100;
                startTime = System.nanoTime();
                for (int j = 0; j < numIterations; j++) {
                    decompositionAlgorithmArticleCEA = new CEADecompositionIntoSymmetricDANsEvaluation(
                            probNets[i], null, preResolutionEvidence);
                    cep = (CEP) decompositionAlgorithmArticleCEA.getUtility().elementTable.get(0);
                }
                endTime = System.nanoTime();
            }
            long totalTime = (endTime - startTime) / numIterations;
            long timeInMiliSeconds = totalTime / 1000000L;
            String infoLine = networkNames[i] + ": " + timeInMiliSeconds + " milisegundos\n";
            System.out.print(infoLine);
            writer.write(infoLine);
        }
        assertTrue(true);
    }
    */
    
    @AfterEach
    public void onTestsEnd() {
        //saveResultsToXSLX();
    }
    
    public void saveResultsToXSLX() throws IOException {
        // Abstract output file
        File resultFile = new File("results.xlsx");
        LogManager.getLogger().debug("Output file: " + resultFile.getAbsolutePath());
        
        // OOXML Excel workbook
        Workbook workbook = new XSSFWorkbook();
        
        // Excel sheet
        Sheet sheet = workbook.createSheet("Execution time");
        
        // Heading row
        int rowNumber = 0;
        Row row = sheet.createRow(rowNumber);
        row.createCell(0).setCellValue("Network name");
        row.createCell(1).setCellValue("DSD UNI time (ns)");
        row.createCell(2).setCellValue("DT UNI time (ns)");
        row.createCell(3).setCellValue("DSD CE time (ns)");
        row.createCell(4).setCellValue("DT CE time (ns)");
        
        rowNumber++;
        for (AnalysisResult result : results) {
            row = sheet.createRow(rowNumber);
            row.createCell(0).setCellValue(result.networkName());
            row.createCell(1).setCellValue(result.dsdUNIEvaluationTime());
            row.createCell(2).setCellValue(result.dtUNIEvaluationTime());
            row.createCell(3).setCellValue(result.dsdCEEvaluationTime());
            row.createCell(4).setCellValue(result.dtCEEvaluationTime());
            rowNumber++;
        }
        
        FileOutputStream outputStream = new FileOutputStream(resultFile);
        workbook.write(outputStream);
        workbook.close();
        
    }
    
    record ProbNetContents(String filename, ProbNet probNet, ProbNetInfo probNetInfo) {
    }
    
    /**
     * Class to store the analysis made for each network
     */
    record AnalysisResult(
            long dsdCEEvaluationTime,
            long dtCEEvaluationTime,
            long dsdUNIEvaluationTime,
            long dtUNIEvaluationTime,
            String networkName,
            
            CEP dsdCEResult,
            CEP dtCEResult,
            TablePotential dsdUNIResult,
            TablePotential dtUNIResult
    ) {
    
    }
    
}
