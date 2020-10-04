package build.archipelago.maui.core.workspace.path;

import build.archipelago.maui.core.exceptions.PathStringInvalidException;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MauiPathTest {

    @Test
    public void testParsePathWithTargetPackage() throws PathStringInvalidException {
        MauiPath.PathProperties pathProperties = MauiPath.PathProperties.parse("[PkgA-1.0]all.test");
        assertEquals("PkgA-1.0", pathProperties.getTargetPackage());
        assertEquals("all", pathProperties.getTransversalType());
        assertEquals("test", pathProperties.getRecipe());
    }
    @Test
    public void testParsePathWithoutTargetPackage() throws PathStringInvalidException {
        MauiPath.PathProperties pathProperties = MauiPath.PathProperties.parse("all.test");
        assertNull(pathProperties.getTargetPackage());
        assertEquals("all", pathProperties.getTransversalType());
        assertEquals("test", pathProperties.getRecipe());

    }
}