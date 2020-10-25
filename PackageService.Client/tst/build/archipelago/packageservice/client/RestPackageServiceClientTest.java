package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.packageservice.client.rest.models.*;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class RestPackageServiceClientTest {

    private PackageServiceClient client;
    private String accountId = "wewelo";

    @Test
    public void testEmptyTest() throws PackageNotFoundException, UnauthorizedException, PackageExistsException, IOException {
        RestPackageServiceClient client = new RestPackageServiceClient("http://localhost:8080",
                "ImRTxga0z1WT6LSdNMRird9R29QQ8GBn",
                "3RjLmcYj1M6jZkEoG6AAMsvtfWkkmt6s3AJgem1xumxcDe_8FVvztwU66D4Maz2M");

        client.createPackage(accountId, CreatePackageRequest.builder()
                .name("CopyBuildSystem")
                .description("The copy build system")
                .build());

        String hash = client.uploadBuiltArtifact(accountId, UploadPackageRequest.builder()
                .pkg(new ArchipelagoPackage("CopyBuildSystem", "1.0"))
                .gitBranch("master")
                .gitCommit("5d3fd1ed75e99f36b92a6e1c9ebd3b21b9b85058")
                .config("version: 1.0\n" +
                        "buildSystem: bash")
                .build(),
                Path.of("C:\\Users\\accou\\workspace\\personal\\TestWorkspaces\\CopyBuildSystem\\build\\bin.zip")
                );



        var getPackage = client.getPackage(accountId, "CopyBuildSystem");
        var builds = client.getPackageBuilds(accountId, new ArchipelagoPackage("CopyBuildSystem", "1.0"));
        var build = client.getPackageBuild(accountId, new ArchipelagoBuiltPackage("CopyBuildSystem", "1.0", hash));
        var pkgByGit = client.getPackageByGit(accountId, "CopyBuildSystem", "master", "5d3fd1ed75e99f36b92a6e1c9ebd3b21b9b85058");
        var artifact = client.getBuildArtifact(accountId, new ArchipelagoBuiltPackage("CopyBuildSystem", "1.0", hash), Path.of("C:\\Users\\accou\\Downloads\\Temp"));
        var verify = client.verifyPackagesExists(accountId, List.of(new ArchipelagoPackage("CopyBuildSystem", "1.0")));
        var verifyBuilds = client.verifyBuildsExists(accountId, List.of(new ArchipelagoBuiltPackage("CopyBuildSystem", "1.0", hash)));

        Assert.assertTrue(true);
    }
//
//    @Before
//    public void setUp() {
//        client = new RestPackageServiceClient("http://localhost:8080");
//    }
//
//    @Test
//    public void testCreate() throws PackageExistsException {
//        String UUID = java.util.UUID.randomUUID().toString().split("-")[0];
//
//        client.createPackage(CreatePackageRequest.builder()
//                .name("KasperTestPackage-" + UUID)
//                .description("This is a description")
//                .build());
//    }
//
//    @Test
//    public void testGet() throws PackageExistsException, PackageNotFoundException {
//        String UUID = java.util.UUID.randomUUID().toString().split("-")[0];
//        String name = "KasperTestPackage-" + UUID;
//
////        client.createPackage(CreatePackageRequest.builder()
////                .name(name)
////                .description("This is a description")
////                .build());
//
//        GetPackageResponse response = client.getPackage("testpackage");
//        Assert.assertEquals("testpackage", response.getName());
//    }
//
//    @Test
//    public void testGetBuilds() throws PackageNotFoundException {
//        var response = client.getPackageBuilds(new ArchipelagoPackage("TestPackage", "1.0"));
//        Assert.assertTrue(response.getBuilds().size() > 0);
//    }
//
//    @Test
//    public void testGetBuild() throws PackageNotFoundException {
//        var res = client.getPackageBuild(new ArchipelagoBuiltPackage("TestPackage", "1.0", "9109e71e"));
//        Assert.assertNotNull(res);
//    }
//
//    @Test
//    public void testVerifyPackages() {
//        var res = client.verifyPackagesExists(List.of(
//                new ArchipelagoPackage("TestPackage", "1.0"),
//                new ArchipelagoPackage("NotExistsTestPackage", "1.0")
//        ));
//        Assert.assertNotNull(res);
//    }
//
//    @Test
//    public void testVerifyBuilds() {
//        var res = client.verifyBuildsExists(List.of(
//                new ArchipelagoBuiltPackage("TestPackage", "1.0", "9109e71e"),
//                new ArchipelagoBuiltPackage("NotExistsTestPackage", "1.0", "blah")
//        ));
//        Assert.assertNotNull(res);
//    }
//
//    @Test
//    public void testUploadBuild() throws PackageNotFoundException {
//        var res = client.uploadBuiltArtifact(UploadPackageRequest.builder()
//                .pkg(new ArchipelagoPackage("TestPackage", "1.1"))
//                .config("this is a nice config").build(), Path.of("C:\\Users\\aoyin\\Downloads\\testZip.zip"));
//        Assert.assertFalse(Strings.isNullOrEmpty(res));
//    }
//
//    @Test
//    public void testDownloadBuildArtifact() throws PackageNotFoundException, IOException {
//        Path base = Paths.get("C:\\Users\\aoyin\\Downloads\\downloadTest");
//        var res = client.getBuildArtifact(
//                new ArchipelagoBuiltPackage("TestPackage", "1.1", "7342ab62"),
//                base);
//        Assert.assertNotNull(res);
//        Assert.assertTrue(Files.exists(res));
//    }
}