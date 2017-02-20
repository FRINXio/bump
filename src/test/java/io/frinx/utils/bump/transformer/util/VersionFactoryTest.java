package io.frinx.utils.bump.transformer.util;

import static com.google.common.collect.Maps.immutableEntry;
import static org.junit.Assert.assertEquals;

import io.frinx.utils.bump.transformer.util.VersionFactory.Version;
import io.frinx.utils.bump.transformer.util.VersionFactory.VersionPart;
import java.util.Optional;
import org.junit.Test;

public class VersionFactoryTest {


    @Test
    public void testStripSnapshot() {
        assertEquals(immutableEntry(false, "1.2.3"), Version.stripSnapshot("1.2.3"));
        assertEquals(immutableEntry(true, "1.2.3"), Version.stripSnapshot("1.2.3-SNAPSHOT"));
    }

    @Test
    public void testParse1() throws Exception {
        assertEquals(new Version("0", 1, Optional.of(VersionPart.parse("5")), Optional.of(1), false, "frinx"),
                VersionFactory.parse("0.1.5.1-frinx", "frinx"));
    }

    @Test
    public void testParse2() throws Exception {
        assertEquals(new Version("0", 1, Optional.of(VersionPart.parse("5")), Optional.of(1), true, "frinx"),
                VersionFactory.parse("0.1.5.1-frinx-SNAPSHOT", "frinx"));
    }

    @Test
    public void testParse3() throws Exception {
        assertEquals(new Version("1", 1, Optional.of(VersionPart.parse("7")), Optional.empty(), false, "frinx"),
                VersionFactory.parse("1.1.7.frinx", "frinx"));
    }

    @Test
    public void testParse4() throws Exception {
        assertEquals(new Version("1", 2, Optional.empty(), Optional.of(3), false, "frinx"),
                VersionFactory.parse("1.2.3-frinx", "frinx"));
    }

    @Test
    public void testParse5() throws Exception {
        assertEquals(new Version("2013.09.07", 8, Optional.of(VersionPart.parse("2")), Optional.of(1), false, "frinx"),
                VersionFactory.parse("2013.09.07.8.2.1-frinx", "frinx"));
    }

    @Test
    public void testParse6() throws Exception {
        assertEquals(new Version("1", 2, Optional.empty(), Optional.of(3), false, "frinxkaraf"),
                VersionFactory.parse("1.2.3-frinxkaraf", "frinxkaraf"));
    }

    @Test
    public void testParse7() throws Exception {
        assertEquals(new Version("0", 3, Optional.of(VersionPart.parse("2-Beryllium-SR2")), Optional.of(2), false, "frinxodl"),
                VersionFactory.parse("0.3.2-Beryllium-SR2.2-frinxodl", "frinxodl"));
    }

    @Test
    public void testParse8() throws Exception {
        assertEquals(new Version("0", 3, Optional.of(VersionPart.parse("3-Beryllium-SR2")), Optional.of(1), false, "frinxodl"),
                VersionFactory.parse("0.3.2-Beryllium-SR2.2-frinxodl", "frinxodl").withMicroIncremented());
    }

}
