package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.RevisionUtil;
import com.google.common.collect.ImmutableList;
import org.junit.*;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CreateVersionSetDelegateTest {

    private CreateVersionSetDelegate delegate;
    private VersionSetService versionSetService;
    private PackageServiceClient packageServiceClient;

    private String testVSName;
    private String parentVSName;
    private String testAccountId = "wewelo";
    private static ArchipelagoPackage pA = ArchipelagoPackage.parse("TestPackageA-1.0");
    private static ArchipelagoPackage pB = ArchipelagoPackage.parse("TestPackageB-1.0");
    private static ArchipelagoPackage pC = ArchipelagoPackage.parse("TestPackageC-1.0");

    @Before
    public void setUp() {
        testVSName = "TestVS-" + RevisionUtil.getRandomRevisionId();
        parentVSName = "parent-" + RevisionUtil.getRandomRevisionId();

        versionSetService = mock(VersionSetService.class);
        packageServiceClient = mock(PackageServiceClient.class);
        delegate = new CreateVersionSetDelegate(versionSetService, packageServiceClient);
    }

    @Test
    public void testCreateValidVersionSet() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException {
        when(packageServiceClient.verifyPackagesExists(eq(testAccountId), any())).thenReturn(
                PackageVerificationResult.<ArchipelagoPackage>builder().missingPackages(ImmutableList.of()).build());
        when(versionSetService.get(eq(testAccountId), eq(parentVSName))).thenReturn(createVS(parentVSName, List.of(pA)));

        delegate.create(testAccountId, testVSName, List.of(pA, pB), Optional.of(parentVSName));
    }

    @Test
    public void testCreateValidVersionSetWithoutParent() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException {
        when(packageServiceClient.verifyPackagesExists(eq(testAccountId), any())).thenReturn(
                PackageVerificationResult.<ArchipelagoPackage>builder().missingPackages(ImmutableList.of()).build());
        delegate.create(testAccountId, testVSName, List.of(pA, pB), Optional.empty());
    }

    @Test(expected = VersionSetExistsException.class)
    public void testCreateVersionSetWithNameThatAlreadyExistsShouldFail() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException {
        when(packageServiceClient.verifyPackagesExists(eq(testAccountId), any())).thenReturn(
                PackageVerificationResult.<ArchipelagoPackage>builder().missingPackages(ImmutableList.of()).build());
        when(versionSetService.get(eq(testAccountId), eq(testVSName))).thenReturn(createVS(testVSName, List.of(pA, pB)));
        delegate.create(testAccountId, testVSName, List.of(pA, pB), Optional.empty());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateVersionWithoutTargetsShouldFail() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException  {
        delegate.create(testAccountId, testVSName, new ArrayList<>(), Optional.empty());
    }

    @Test(expected = PackageNotFoundException.class)
    public void testCreateVersionWithTargetThatDoseNotExistsShouldFail() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException  {
        String packageName = "DoseNotExists-1.0";
        when(packageServiceClient.verifyPackagesExists(eq(testAccountId), any())).thenReturn(
                PackageVerificationResult.<ArchipelagoPackage>builder().missingPackages(
                        ImmutableList.of(ArchipelagoPackage.parse(packageName))).build());
        when(versionSetService.get(eq(testAccountId), eq(testVSName))).thenReturn(null);

        delegate.create(testAccountId, testVSName, List.of(ArchipelagoPackage.parse(packageName)), Optional.empty());
    }


    private VersionSet createVS(String vsName, List<ArchipelagoPackage> targets) {
        Instant created = Instant.now();
        String vsParentName = "parent-master";
        String revisionId = "123";
        Instant revisionDate = Instant.now();
        Revision revisionA = Revision.builder()
                .revisionId(revisionId)
                .created(revisionDate)
                .build();
        return VersionSet.builder()
                .name(vsName)
                .created(created)
                .parent(vsParentName)
                .targets(targets)
                .revisions(List.of(revisionA))
                .latestRevisionCreated(revisionDate)
                .latestRevision(revisionId)
                .build();
    }
}