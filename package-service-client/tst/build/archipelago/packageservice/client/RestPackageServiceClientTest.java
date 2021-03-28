package build.archipelago.packageservice.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.models.UploadPackageRequest;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.packageservice.exceptions.*;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Paths;

public class RestPackageServiceClientTest {

    private PackageServiceClient client;
    private String accountId = "wewelo";

    @Test
    public void testEmptyTest() throws PackageNotFoundException, UnauthorizedException, PackageExistsException, IOException {

        client = new RestPackageServiceClient("http://localhost:8090",
                "4ok75klvst0vf15lel5sdbdoc0",
                "1u7m9pa9njct19jbj51b592ci0ku151quhjo4n0pcm36h3lude91");

        String packageName = "PleaseDeleteMe";
//
//        client.createPackage("wewelo", CreatePackageRequest.builder()
//                .name(packageName)
//                .description("Delete him for god's sake")
//                .build());

        String hash = client.uploadBuiltArtifact("wewelo", UploadPackageRequest.builder()
                .config("Blah blah")
                .pkg(new ArchipelagoPackage(packageName, "1.Delete"))
                .gitBranch("DeleteTheBranch")
                .gitCommit("DeleteCommit")
                .build(), Paths.get("C:\\Users\\accou\\Downloads\\OpenJDK11U-jdk_x64_windows_hotspot_11.0.10_9.msi"));

        System.out.println("Hash");
        Assert.assertTrue(true);
    }
}