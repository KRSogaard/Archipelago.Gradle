package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import org.junit.*;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GetVersionSetDelegateTest {

    private GetVersionSetDelegate delegate;
    private VersionSetService versionSetService;
    private String testAccountId = "wewelo";

    @Before
    public void setUp() {
        versionSetService = mock(VersionSetService.class);
        delegate = new GetVersionSetDelegate(versionSetService);
    }

    @Test
    public void getVersionSetThatExists() throws VersionSetDoseNotExistsException {
        String vsName = "TestVS-master";
        Instant created = Instant.now();
        String vsParentName = "TestParentVS/master";
        ArchipelagoPackage pA = ArchipelagoPackage.parse("TestPackageA-1.0");
        ArchipelagoPackage pB = ArchipelagoPackage.parse("TestPackageB-1.0");
        ArchipelagoPackage pC = ArchipelagoPackage.parse("TestPackageC-1.0");
        String revisionId = "123";
        Instant revisionDate = Instant.now();
        Revision revisionA = Revision.builder()
                .revisionId(revisionId)
                .created(revisionDate)
                .build();
        VersionSet vs = VersionSet.builder()
                .name(vsName)
                .created(created)
                .parent(vsParentName)
                .target(pA)
                .revisions(List.of(revisionA))
                .latestRevisionCreated(revisionDate)
                .latestRevision(revisionId)
                .build();
        when(versionSetService.get(eq(testAccountId), eq(vsName))).thenReturn(vs);
        VersionSet r = delegate.getVersionSet(testAccountId, vsName);
        // We validate the data instead of the VS ref as we want to allow the delegate to create a new object

        Assert.assertNotNull(r);
        Assert.assertEquals(vsName, r.getName());
        Assert.assertEquals(created, r.getCreated());
        Assert.assertNotNull(r.getParent());
        Assert.assertEquals(vsParentName, r.getParent());
        Assert.assertNotNull(r.getTarget());
        Assert.assertEquals(r.getTarget(), pA);
        Assert.assertNotNull(r.getRevisions());
        Assert.assertEquals(1, r.getRevisions().size());
        Assert.assertTrue(r.getRevisions().stream().anyMatch(x -> revisionA.getRevisionId().equals(x.getRevisionId())));
        Assert.assertNotNull(r.getLatestRevisionCreated());
        Assert.assertEquals(revisionDate, r.getLatestRevisionCreated());
        Assert.assertNotNull(r.getLatestRevision());
        Assert.assertEquals(revisionId, r.getLatestRevision());
    }

    @Test(expected = VersionSetDoseNotExistsException.class)
    public void getVersionSetThatDoseNotExists() throws VersionSetDoseNotExistsException {
        String vsName = "TestVS-master";
        when(versionSetService.get(eq(testAccountId), eq(vsName))).thenReturn(null);
        delegate.getVersionSet(testAccountId, vsName);
    }
}