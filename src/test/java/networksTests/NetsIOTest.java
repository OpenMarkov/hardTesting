/*
 * Copyright (c) CISIAD, UNED, Spain,  2018. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package networksTests;

import bitbucket.NetsRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openmarkov.core.exception.ParserException;
import org.openmarkov.core.exception.WriterException;
import org.openmarkov.core.io.ProbNetInfo;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.gui.dialog.io.NetsIO;
import org.openmarkov.inference.InferenceTestsTools;
import org.openmarkov.integrationTests.IntegrationTest;
import org.openmarkov.io.probmodel.reader.PGMXReader_0_2;
import org.openmarkov.io.probmodel.reader.PGMXReader_1_0;
import org.openmarkov.io.probmodel.writer.PGMXWriter_0_2;
import org.openmarkov.io.probmodel.writer.PGMXWriter_1_0;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * This class tests the class
 *
 * @author jmendoza
 * @author mkpalacio
 * @author jperez
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class NetsIOTest {
    
    private static final Set<String> NETWORKS_TO_SKIP = NetsIOTest.networksToSkip();
    
    private static @NotNull Set<String> networksToSkip() {
        return Stream.of(
                //Already passed with VEPropagation
                "BN-alarm.pgmx",
                "BN-asia.pgmx",
                "BN-catarnet.pgmx",
                "BN-hepar.pgmx",
                "BN-nasonet.pgmx",
                "BN-one-disease.pgmx",
                "BN-prostanet.pgmx",
                "BN-two-diseases.pgmx",
                "BN-two-diseases-naive.pgmx",
                "BN-noisy-or-four-parents.pgmx",
                //Already passed with load, save and reload
                "DAN-3-test-problem.pgmx",
                "DAN-4-test-problem.pgmx",
                "DAN-5-test-problem.pgmx",
                "DAN-6-test-problem.pgmx",
                "DAN-7-test-problem.pgmx",
                "DAN-arthronet.pgmx",
                "DAN-dating.pgmx",
                "DAN-decide-test-ce.pgmx",
                "DAN-decide-test-ordered.pgmx",
                "DAN-decide-test-symptom.pgmx",
                "DAN-decide-test-with-restrictive-symptom.pgmx",
                "DAN-decide-test-with-symptom.pgmx",
                "DAN-decide-test.pgmx",
                "DAN-decide-test-2therapies.pgmx",
                "DAN-delayed-result-of-test.pgmx",
                "DAN-diabetes.pgmx",
                "DAN-economic-mediastinet.pgmx",
                "DAN-king.pgmx",
                "DAN-mediastinet.pgmx",
                "DAN-mediastinet-ce.pgmx",
                "DAN-symmetric-test1.pgmx",
                "DAN-qale-mediastinet.pgmx",
                "DAN-reactor.pgmx",
                "DAN-test-always.pgmx",
                "DAN-test-2therapies.pgmx",
                "DAN-unordered-two-decs.pgmx",
                "DAN-used-car-buyer.pgmx",
                "LIMID-Nilsson-Lauritzen.pgmx",
                "LIMID-decide-test-symptom.pgmx",
                "Dec-POMDP-wireless-network.pgmx",
                "POMDP-coffee-robot.pgmx",
                // TODO - Check CEA: Already passed with VEResolution, VEPropagation, VETemporalEvolution, VECEADecision, VECEAGlobal, VECEPSA
                "ID-CEA-minimal.pgmx",
                //"ID-CEA-test-2therapies-3criteria.pgmx",
                "ID-CEA-test-2therapies-new-test.pgmx",
                "ID-CEA-test-2therapies.pgmx",
                "ID-decide-test-without-dummy-state.pgmx",
                "ID-decide-test.pgmx",
                "ID-Monty-Hall-spanish.pgmx",
                "MID-Chancellor.pgmx",
                "MID-Chancellor-new.pgmx",
                "MID-Chancellor-corrected.pgmx",
                "MID-mammography.pgmx",
                "MID-hip-Briggs.pgmx",
                "MID-dmhee-2.5.pgmx",
                "MID-dmhee-3.5.pgmx",
                "MID-dmhee-4.7.pgmx",
                "MID-dmhee-4.8.pgmx",
                "MID-HPV-without-supervalue.pgmx",
                // TODO - Failed on VEPropagation (Draw/Tie Policy ?)
                "ID-delayed-result-of-test.pgmx",
                // TODO - Failed getting optimal intervention on Resolution
                "ID-mediastinet-ce.pgmx",
                
                // TODO - Failed on Resolution?
                "MID-CHD-Walker.pgmx",
                
                // TODO - Failed in VEPropagation (All with supervalue nodes)
                "ID-arthronet.pgmx",
                "ID-arthronet-ce.pgmx",
                "ID-mediastinet.pgmx",
                "ID-used-car-buyer.pgmx",
                "MID-CHAP-Ryan-Griffin.pgmx",
                "MID-HPV.pgmx",
                // Too big
                "MID-Cochlear.pgmx",
                "MID-Colorectal.pgmx",
                // TODO - only for 'Augmented bayesian networks' branch
                "ID-decide-test-0.4.0.pgmx",
                "ID-decide-test-0.5.0.pgmx",
                
                //TODO: These are quite slow to execute
                "MID-CHAP-Ryan-Griffin-1-0.pgmx",
                "MID-Cochlear-1-0.pgmx",
                "MID-HPV-1-0.pgmx",
                "MID-HPV-without-supervalue-1-0.pgmx",
                "MID-hip-Briggs-1-0.pgmx"
        ).collect(Collectors.toSet());
    }
    
    record NetworkToTest(URL url, PGMXVersion version) {}
    
    static Stream<NetworkToTest> networksToTest() throws IOException {
        NetsRepository repository = new NetsRepository();
        List<URL> listURL = repository.getNetworks();
        listURL.removeIf(url -> {
            String networkName = url.getPath();
            networkName = networkName.substring(networkName.lastIndexOf("/") + 1);
            return NETWORKS_TO_SKIP.contains(networkName);
        });
        ArrayList<NetworkToTest> listNetworkToTest = new ArrayList<>(listURL.size());
        for(URL url : listURL) {
            PGMXVersion version = getVersion(url);
            listNetworkToTest.add(new NetworkToTest(url, version));
        }
        return listNetworkToTest.stream();
    }
    
    static Stream<NetworkToTest> networksToTestOfVersion0_2() throws IOException {
        return NetsIOTest.networksToTest().filter(networkToTest -> networkToTest.version == PGMXVersion.V0_2_0);
    }
    
    private static PGMXVersion getVersion(URL url) throws IOException {
        Pattern versionPattern = Pattern.compile("<ProbModelXML.*formatVersion\\s*=\\s*\"(.*)\".*>");
        String fileContents = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
        Matcher versionMatcher = versionPattern.matcher(fileContents);
        if (!versionMatcher.find()) {
            return PGMXVersion.V0_2_0;
        }
        String versionString = versionMatcher.group(1);
        if (versionString.startsWith("1.0")) {
            return PGMXVersion.V1_0_0;
        }
        return PGMXVersion.V0_2_0;
    }
    
    /**
     * This method opens the a net from a file and saves it into another file.
     * It makes the asserts to verify if the tests are good.
     *
     * @param fileToOpen the file from which open the network.
     * @param fileToSave the file into which save the network.
     * @throws Exception if an error has occurred.
     */
    private void openSaveNetwork(String fileToOpen, String fileToSave) throws Exception {
        URL resource = new IntegrationTest().getClass().getClassLoader().getResource(fileToOpen);
        if (resource == null) {
            throw new Exception("Could not load network: " + fileToOpen);
        }
        File file = new File(resource.toURI());
        String fileNameOpen = file.getAbsolutePath();
        if (fileNameOpen == null) {
            fail("The test file " + fileNameOpen + " can't be found");
        }
        ProbNet net = NetsIO.openNetworkFile(fileNameOpen).getProbNet();
        assertNotNull(net);
        String path = file.getParent();
        String fileNameSave = path + File.separator + fileToSave;
        NetsIO.saveNetworkFile(net, fileNameSave, "");
        net = NetsIO.openNetworkFile(fileNameSave).getProbNet();
        assertNotNull(net);
        new File(fileNameSave).deleteOnExit();
    }
    
    /**
     * This method tests the methods 'openNetworkFile' and 'saveNetworkFile',
     * opening and saving various files that contain bayes nets and influence
     * diagrams.
     *
     * @throws Exception if an error occurred while the networks are opened and
     *                   saved.
     */
    @Test public final void testOpenSaveNetworkFile() throws Exception {
        openSaveNetwork("Net1.elv", "Net1Saved.elv");
        openSaveNetwork("Net2.elv", "Net2Saved.elv");
    }
    
    @ParameterizedTest
    @MethodSource("networksToTest")
    public final void testOpenSaveRepositoryNets(NetworkToTest networkToTest) throws IOException, ParserException, WriterException {
        // The name is irrelevant because this nets will only be created for tests purposes and it will be deleted
        // after each iteration
        
        
        String networkName = networkToTest.url.getPath();
        networkName = networkName.substring(networkName.lastIndexOf("/") + 1);
        
        PGMXVersion pgmxVersion = networkToTest.version;
        
        
        PGMXReader_0_2 pgmxReader = pgmxVersion.reader();
        
        ProbNetInfo probNetInfo = pgmxReader.loadProbNetInfo(networkName, networkToTest.url.openStream());
        ProbNet probNet = probNetInfo.getProbNet();
        assertNotNull(probNet);
        assertNotNull(probNet.getNodes());
        
        PGMXWriter_0_2 pgmxWritter = pgmxVersion.writer();
        pgmxWritter.writeProbNet(networkName, probNet, probNetInfo.getEvidence());
        new File(networkName).deleteOnExit();
        
        FileInputStream file = new FileInputStream(networkName);
        probNetInfo = pgmxReader.loadProbNetInfo(networkName, file);
        probNet = probNetInfo.getProbNet();
        System.out.println("Loaded, saved and reloaded probNet:" + networkToTest.url.getPath());
        assertNotNull(probNet);
        assertNotNull(probNet.getNodes());
        EvidenceCase preResolutionEvidence;
        int numSimulations = 10;
        boolean useMultithreading = true;
        
        if (!probNetInfo.getEvidence().isEmpty()) {
            preResolutionEvidence = probNetInfo.getEvidence().get(0);
        } else {
            preResolutionEvidence = new EvidenceCase();
        }
        InferenceTestsTools.testBasicInference(probNet, preResolutionEvidence, numSimulations, useMultithreading);
    }
    
    @ParameterizedTest
    @MethodSource("networksToTestOfVersion0_2")
    public void testPGMX_0_2vs0_5(NetworkToTest networkToTest) throws IOException, WriterException, ParserException {
        // The name is irrelevant because this nets will only be created for tests purposes and it will be deleted
        // after each iteration
        String networkName = networkToTest.url.getPath();
        networkName = networkName.substring(networkName.lastIndexOf("/") + 1);
        
        
        // Load probNet in 0_2
        PGMXReader_0_2 pgmxReader_0_2 = new PGMXReader_0_2();
        ProbNetInfo probNetInfo_0_2 = pgmxReader_0_2.loadProbNetInfo(networkName, networkToTest.url.openStream());
        ProbNet probNet_0_2 = probNetInfo_0_2.getProbNet();
        List<EvidenceCase> evidenceCase_0_2 = probNetInfo_0_2.getEvidence();
        assertNotNull(probNet_0_2);
        assertNotNull(evidenceCase_0_2);
        
        // Write probNet in 0_5 version and check integrity
        PGMXWriter_1_0 pgmxWritter = new PGMXWriter_1_0();
        pgmxWritter.writeProbNet(networkName, probNet_0_2, evidenceCase_0_2);
        new File(networkName).deleteOnExit();
        
        // Re-open netwokr in 0_5
        FileInputStream file = new FileInputStream(networkName);
        PGMXReader_1_0 pgmxReader_0_5 = new PGMXReader_1_0();
        ProbNetInfo probNetInfo_0_5 = pgmxReader_0_5.loadProbNetInfo(networkName, file);
        ProbNet probNet_0_5 = probNetInfo_0_5.getProbNet();
        List<EvidenceCase> evidenceCase_0_5 = probNetInfo_0_5.getEvidence();
        assertNotNull(probNet_0_5);
        assertNotNull(evidenceCase_0_5);
        for (int i = 0; i < evidenceCase_0_2.size(); i++) {
            assertEquals(evidenceCase_0_2.get(0).getNumberOfFindings(), evidenceCase_0_5.get(0).getNumberOfFindings());
        }
    }
    
    
    enum PGMXVersion {
        V0_2_0,
        V1_0_0;
        
        PGMXReader_0_2 reader() {
            return switch (this) {
                case V0_2_0 -> new PGMXReader_0_2();
                case V1_0_0 -> new PGMXReader_1_0();
            };
        }
        
        PGMXWriter_0_2 writer() {
            return switch (this) {
                case V0_2_0 -> new PGMXWriter_0_2();
                case V1_0_0 -> new PGMXWriter_1_0();
            };
        }
    }
}
