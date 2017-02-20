package io.frinx.utils.bump.transformer;

import static org.junit.Assert.assertEquals;

import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.SnapshotTransformation;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionBump;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionTransformationStrategy;
import io.frinx.utils.bump.transformer.util.VersionFactory.Version.VersionParsingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.Test;

public class VersionTransformationStrategyFactoryTest {

    @Test
    public void testParseArgs_withDefaults() {
        Entry<VersionTransformationStrategy, List<String>> entry = VersionTransformationStrategyFactory.parseArgs(
                Arrays.asList("--suffix", "frinxinstaller", "someargs"));
        VersionTransformationStrategy tested = entry.getKey();
        assertEquals(new VersionTransformationStrategy("frinxinstaller", VersionBump.NONE, SnapshotTransformation.NONE,
                        Optional.empty(), Optional.empty()),
                tested);
        assertEquals(Arrays.asList("someargs"), entry.getValue());
    }

    @Test
    public void testParseArgs() {
        Entry<VersionTransformationStrategy, List<String>> entry = VersionTransformationStrategyFactory.parseArgs(
                Arrays.asList("--suffix", "frinxinstaller", "--bump", "qualifier",
                        "--postprocess-sed", "s/a/b/", "--snapshot", "flip", "--preprocess-sed", "s/b/a", "someargs"));
        VersionTransformationStrategy tested = entry.getKey();
        assertEquals(new VersionTransformationStrategy("frinxinstaller", VersionBump.QUALIFIER, SnapshotTransformation.FLIP,
                        Optional.of("s/a/b/"), Optional.of("s/b/a")),
                tested);
        assertEquals(Arrays.asList("someargs"), entry.getValue());
    }

    @Test
    public void testTransformation() throws VersionParsingException {
        VersionTransformationStrategy transformationStrategy = new VersionTransformationStrategy("frinx",
                VersionBump.MICRO, SnapshotTransformation.FLIP,
                Optional.of("s/frinx/rc2-frinx/"), Optional.of("s/rc1-frinx/frinx/"));

        String transformed = transformationStrategy.transform("1.2.3.rc1-frinx");
        assertEquals("1.2.4.rc2-frinx-SNAPSHOT", transformed);
    }
}
