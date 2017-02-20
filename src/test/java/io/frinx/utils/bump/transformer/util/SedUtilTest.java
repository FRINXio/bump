package io.frinx.utils.bump.transformer.util;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

public class SedUtilTest {

    @Test
    public void testSed() throws IOException, InterruptedException {
        assertEquals("aBc", SedUtil.sed("abc", "s/b/B/"));
    }
}