package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.delegates.createVersionSet.CreateVersionSetDelegate;
import build.archipelago.versionsetservice.core.delegates.createVersionSet.CreateVersionSetRequest;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.RevisionUtil;
import build.archipelago.versionsetservice.exceptions.*;
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

    @Test(expected = PackageNotFoundException.class)
    public void testCreateVersionWithTargetThatDoseNotExistsShouldFail() throws VersionSetExistsException,
            VersionSetDoseNotExistsException, PackageNotFoundException {
        String packageName = "DoseNotExists-1.0";
        when(packageServiceClient.verifyPackagesExists(eq(testAccountId), any())).thenReturn(
                PackageVerificationResult.<ArchipelagoPackage>builder().missingPackages(
                        ImmutableList.of(ArchipelagoPackage.parse(packageName))).build());
        when(versionSetService.get(eq(testAccountId), eq(testVSName))).thenThrow(new VersionSetDoseNotExistsException(testVSName));

        delegate.create(CreateVersionSetRequest.builder()
                .accountId(testAccountId)
                .name(testVSName)
                .target(ArchipelagoPackage.parse(packageName))
                .build());
    }


    private VersionSet createVS(String vsName, ArchipelagoPackage target) {
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
                .target(target)
                .revisions(List.of(revisionA))
                .latestRevisionCreated(revisionDate)
                .latestRevision(revisionId)
                .build();
    }
}