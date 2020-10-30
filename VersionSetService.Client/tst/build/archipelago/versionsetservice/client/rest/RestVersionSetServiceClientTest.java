package build.archipelago.versionsetservice.client.rest;

import build.archipelago.common.exceptions.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import org.junit.*;

public class RestVersionSetServiceClientTest {

    private VersionSetServiceClient client;
    private String accountId = "wewelo";
    private String vsName = "TestVS";

    @Before
    public void setUp() throws Exception {
       // client = new RestVersionSetSetServiceClient("http://localhost:8081");
    }

    @Test
    public void testEmptyTest() throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException, MissingTargetPackageException {
//        VersionSetServiceClient client = new RestVersionSetSetServiceClient("http://localhost:8090",
//                "ImRTxga0z1WT6LSdNMRird9R29QQ8GBn",
//                "3RjLmcYj1M6jZkEoG6AAMsvtfWkkmt6s3AJgem1xumxcDe_8FVvztwU66D4Maz2M");
//
//        client.createVersionSet(accountId, CreateVersionSetRequest.builder()
//                .name(vsName)
//                .targets(List.of(new ArchipelagoPackage("CopyBuildSystem", "1.0")))
//                .build());
//
//        var vs = client.getVersionSet(accountId, vsName);
//        var hash = client.createVersionRevision(accountId, vsName, List.of(new ArchipelagoBuiltPackage("CopyBuildSystem", "1.0", "4da09c69")));
//        var vsAfter = client.getVersionSet(accountId, vsName);
//        var packages = client.getVersionSetPackages(accountId, vsName, hash);
//

        Assert.assertTrue(true);
    }

//    @Test
//    public void testCreateVersionSet() throws VersionSetExistsException, VersionSetDoseNotExistsException,
//            PackageNotFoundException {
//        String name = "TestVersionSet-" + UUID.randomUUID().toString().split("-")[0];
//        client.createVersionSet(CreateVersionSetRequest.builder()
//                .name(name)
//                .targets(ImmutableList.of(new ArchipelagoPackage("testpackage", "1.0")))
//                .build());
//    }
//
//    @Test
//    public void testCreateVersionSetRevision() throws MissingTargetPackageException, VersionSetDoseNotExistsException, PackageNotFoundException {
//        String name = "testversionset-0c5a5ff9";
//        String revision = client.createVersionRevision(name,
//                ImmutableList.of(new ArchipelagoBuiltPackage("testpackage", "1.0", "9109e71e")));
//
//        Assert.assertNotNull(revision);
//    }
//
//    @Test
//    public void testGetVersionSet() throws VersionSetDoseNotExistsException {
//        String name = "testversionset-4f1b92d2";
//        VersionSet vs = client.getVersionSet(name);
//
//        Assert.assertNotNull(vs);
//    }
//
//    @Test
//    public void testGetVersionSetRevision() throws VersionSetDoseNotExistsException {
//        String name = "testversionset-0c5a5ff9";
//        String revision = "3494d4f0";
//
//        var revisionObj = client.getVersionSetPackages(name, revision);
//        Assert.assertNotNull(revisionObj);
//    }
}