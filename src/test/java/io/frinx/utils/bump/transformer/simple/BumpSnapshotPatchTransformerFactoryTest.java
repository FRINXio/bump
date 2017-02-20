package io.frinx.utils.bump.transformer.simple;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.SnapshotTransformation;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionBump;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionTransformationStrategy;
import io.frinx.utils.bump.transformer.simple.BumpSnapshotPatchTransformerFactory.BumpSnapshotPatchTransformer;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class BumpSnapshotPatchTransformerFactoryTest {


    @Test
    public void testPomFrixBump() {
        List<String> input = Arrays.asList(
                "-      <version>1.1.1.3-frinx</version>",
                "+      <version>1.1.1.3-frinx-SNAPSHOT</version>");
        VersionTransformationStrategy strategy = new VersionTransformationStrategy("frinx", VersionBump.QUALIFIER,
                SnapshotTransformation.ADD, Optional.empty(), Optional.empty());
        BumpSnapshotPatchTransformer tested = BumpSnapshotPatchTransformerFactory.create(strategy);
        List<String> result = tested.fixLines(input, new File(""));
        List<String> expected = Arrays.asList(
                "-      <version>1.1.1.3-frinx</version>",
                "+      <version>1.1.1.4-frinx-SNAPSHOT</version>");
        assertEquals(expected, result);
    }

    @Test
    public void testPomMicroBump_disto() {
        List<String> input = Arrays.asList(
                "-      <version>1.2.0.frinx</version>",
                "+      <version>1.1.8.frinx-SNAPSHOT</version>");
        VersionTransformationStrategy strategy = new VersionTransformationStrategy("frinx", VersionBump.MICRO,
                SnapshotTransformation.ADD, Optional.empty(), Optional.empty());
        BumpSnapshotPatchTransformer tested = BumpSnapshotPatchTransformerFactory.create(strategy);
        List<String> result = tested.fixLines(input, new File(""));
        List<String> expected = Arrays.asList(
                "-      <version>1.2.0.frinx</version>",
                "+      <version>1.2.1.frinx-SNAPSHOT</version>");
        assertEquals(expected, result);
    }

    @Test
    public void testFeaturesMicroBump() {
        List<String> input = Arrays.asList(
                "-      <feature version='1.3.2.2-frinx'>odl-mdsal-broker</feature>",
                "+      <feature version='1.3.2.2-frinx-SNAPSHOT'>odl-mdsal-broker</feature>");
        VersionTransformationStrategy strategy = new VersionTransformationStrategy("frinx", VersionBump.MICRO,
                SnapshotTransformation.ADD, Optional.empty(), Optional.empty());
        BumpSnapshotPatchTransformer tested = BumpSnapshotPatchTransformerFactory.create(strategy);
        List<String> result = tested.fixLines(input, new File(""));
        List<String> expected = Arrays.asList(
                "-      <feature version='1.3.2.2-frinx'>odl-mdsal-broker</feature>",
                "+      <feature version='1.3.3.1-frinx-SNAPSHOT'>odl-mdsal-broker</feature>");
        assertEquals(expected, result);
    }

    @Test
    public void testFeaturesFrixBump() {
        List<String> input = Arrays.asList(
                "-      <feature version='1.3.2.2-frinx'>odl-mdsal-broker</feature>",
                "+      <feature version='1.3.2.2-frinx-SNAPSHOT'>odl-mdsal-broker</feature>");
        VersionTransformationStrategy strategy = new VersionTransformationStrategy("frinx", VersionBump.QUALIFIER,
                SnapshotTransformation.ADD, Optional.empty(), Optional.empty());
        BumpSnapshotPatchTransformer tested = BumpSnapshotPatchTransformerFactory.create(strategy);
        List<String> result = tested.fixLines(input, new File(""));
        List<String> expected = Arrays.asList(
                "-      <feature version='1.3.2.2-frinx'>odl-mdsal-broker</feature>",
                "+      <feature version='1.3.2.3-frinx-SNAPSHOT'>odl-mdsal-broker</feature>");
        assertEquals(expected, result);
    }

    void assertTransformation(String inputString, String expectedString) {
        List<String> input = Lists.newArrayList(Splitter.on('\n').split(inputString));
        VersionTransformationStrategy strategy = new VersionTransformationStrategy("frinx", VersionBump.QUALIFIER,
                SnapshotTransformation.ADD, Optional.empty(), Optional.empty());
        BumpSnapshotPatchTransformer tested = BumpSnapshotPatchTransformerFactory.create(strategy);
        List<String> result = tested.fixLines(input, new File(""));
        result.forEach(System.out::println);
        assertEquals(countLinesStartingWith(input, "-"), countLinesStartingWith(result, "-"));
        assertEquals(countLinesStartingWith(input, "+"), countLinesStartingWith(result, "+"));

        List<String> expected = Lists.newArrayList(Splitter.on('\n').split(expectedString));
        assertEquals(expected, result);
    }

    @Test
    public void testMultipleLinesFrinxBump() {
        String inputString = "" +
                "     <properties>\n" +
                "-        <config.version>0.4.2.1-frinx</config.version>\n" +
                "-        <mdsal.version>2.0.2.1-frinx</mdsal.version>\n" +
                "-        <controller.mdsal.version>1.3.2.1-frinx</controller.mdsal.version>\n" +
                "-        <yangtools.version>0.8.2.1-frinx</yangtools.version>\n" +
                "+        <config.version>0.4.2.1-frinx-SNAPSHOT</config.version>\n" +
                "+        <mdsal.version>2.0.2.1-frinx-SNAPSHOT</mdsal.version>\n" +
                "+        <controller.mdsal.version>1.3.2.1-frinx-SNAPSHOT</controller.mdsal.version>\n" +
                "+        <yangtools.version>0.8.2.1-frinx-SNAPSHOT</yangtools.version>\n" +
                "     </properties>";
        String expectedString = "" +
                "     <properties>\n" +
                "-        <config.version>0.4.2.1-frinx</config.version>\n" +
                "-        <mdsal.version>2.0.2.1-frinx</mdsal.version>\n" +
                "-        <controller.mdsal.version>1.3.2.1-frinx</controller.mdsal.version>\n" +
                "-        <yangtools.version>0.8.2.1-frinx</yangtools.version>\n" +
                "+        <config.version>0.4.2.2-frinx-SNAPSHOT</config.version>\n" +
                "+        <mdsal.version>2.0.2.2-frinx-SNAPSHOT</mdsal.version>\n" +
                "+        <controller.mdsal.version>1.3.2.2-frinx-SNAPSHOT</controller.mdsal.version>\n" +
                "+        <yangtools.version>0.8.2.2-frinx-SNAPSHOT</yangtools.version>\n" +
                "     </properties>";
        assertTransformation(inputString, expectedString);
    }

    @Test
    public void testBuggyReplace() throws Exception {
        String inputString = "" +
                "-    <usermanager.implementation.version>0.6.2.1-frinx</usermanager.implementation.version>\n" +
                "-    <usermanager.northbound.version>0.2.2.1-frinx</usermanager.northbound.version>\n" +
                "-    <usermanager.version>0.6.2.1-frinx</usermanager.version>\n" +
                "-    <nsf.version>0.6.2.1-frinx</nsf.version>\n" +
                "-    <web.version>0.6.2.1-frinx</web.version>\n" +
                "-    <yang-ext.version>2013.09.07.8.2.1-frinx</yang-ext.version>\n" +
                "-    <yang-jmx-generator.version>1.2.2.1-frinx</yang-jmx-generator.version>\n" +
                "-    <yangtools.version>0.8.2.1-frinx</yangtools.version>\n" +
                "+    <usermanager.implementation.version>0.6.2.1-frinx-SNAPSHOT</usermanager.implementation.version>\n" +
                "+    <usermanager.northbound.version>0.2.2.1-frinx-SNAPSHOT</usermanager.northbound.version>\n" +
                "+    <usermanager.version>0.6.2.1-frinx-SNAPSHOT</usermanager.version>\n" +
                "+    <nsf.version>0.6.2.1-frinx-SNAPSHOT</nsf.version>\n" +
                "+    <web.version>0.6.2.1-frinx-SNAPSHOT</web.version>\n" +
                "+    <yang-ext.version>2013.09.07.8.2.1-frinx-SNAPSHOT</yang-ext.version>\n" +
                "+    <yang-jmx-generator.version>1.2.2.1-frinx-SNAPSHOT</yang-jmx-generator.version>\n" +
                "+    <yangtools.version>0.8.2.1-frinx-SNAPSHOT</yangtools.version>";

        String expectedString = "" +
                "-    <usermanager.implementation.version>0.6.2.1-frinx</usermanager.implementation.version>\n" +
                "-    <usermanager.northbound.version>0.2.2.1-frinx</usermanager.northbound.version>\n" +
                "-    <usermanager.version>0.6.2.1-frinx</usermanager.version>\n" +
                "-    <nsf.version>0.6.2.1-frinx</nsf.version>\n" +
                "-    <web.version>0.6.2.1-frinx</web.version>\n" +
                "-    <yang-ext.version>2013.09.07.8.2.1-frinx</yang-ext.version>\n" +
                "-    <yang-jmx-generator.version>1.2.2.1-frinx</yang-jmx-generator.version>\n" +
                "-    <yangtools.version>0.8.2.1-frinx</yangtools.version>\n" +
                "+    <usermanager.implementation.version>0.6.2.2-frinx-SNAPSHOT</usermanager.implementation.version>\n" +
                "+    <usermanager.northbound.version>0.2.2.2-frinx-SNAPSHOT</usermanager.northbound.version>\n" +
                "+    <usermanager.version>0.6.2.2-frinx-SNAPSHOT</usermanager.version>\n" +
                "+    <nsf.version>0.6.2.2-frinx-SNAPSHOT</nsf.version>\n" +
                "+    <web.version>0.6.2.2-frinx-SNAPSHOT</web.version>\n" +
                "+    <yang-ext.version>2013.09.07.8.2.2-frinx-SNAPSHOT</yang-ext.version>\n" +
                "+    <yang-jmx-generator.version>1.2.2.2-frinx-SNAPSHOT</yang-jmx-generator.version>\n" +
                "+    <yangtools.version>0.8.2.2-frinx-SNAPSHOT</yangtools.version>";
        assertTransformation(inputString, expectedString);
    }

    long countLinesStartingWith(List<String> lines, String prefix) {
        return lines.stream().filter(s -> s.startsWith(prefix)).count();
    }

}
