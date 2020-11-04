package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import org.junit.*;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GetVersionSetDelegateTest {

    private GetVersionSetDelegate delegate;
    private VersionSetService versionSetService;

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
        List<ArchipelagoPackage> targets = List.of(pA, pB, pC);
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
                .targets(targets)
                .revisions(List.of(revisionA))
                .latestRevisionCreated(revisionDate)
                .latestRevision(revisionId)
                .build();
        when(versionSetService.get(accountId, eq(vsName))).thenReturn(vs);
        VersionSet r = delegate.getVersionSet(vsName);
        // We validate the data instead of the VS ref as we want to allow the delegate to create a new object

        Assert.assertNotNull(r);
        Assert.assertEquals(vsName, r.getName());
        Assert.assertEquals(created, r.getCreated());
        Assert.assertNotNull(r.getParent() );
        Assert.assertEquals(vsParentName, r.getParent());
        Assert.assertNotNull(r.getTargets());
        Assert.assertEquals(3, r.getTargets().size());
        Assert.assertTrue(r.getTargets().stream().anyMatch(x -> pA.toString().equals(x.toString())));
        Assert.assertTrue(r.getTargets().stream().anyMatch(x -> pB.toString().equals(x.toString())));
        Assert.assertTrue(r.getTargets().stream().anyMatch(x -> pC.toString().equals(x.toString())));
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
        when(versionSetService.get(accountId, eq(vsName))).thenReturn(null);
        delegate.getVersionSet(vsName);
    }
}