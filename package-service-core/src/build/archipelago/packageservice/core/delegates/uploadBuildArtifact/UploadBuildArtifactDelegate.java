package build.archipelago.packageservice.core.delegates.uploadBuildArtifact;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.storage.PackageStorage;
import build.archipelago.packageservice.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;

@Slf4j
public class UploadBuildArtifactDelegate {

    private PackageStorage packageStorage;
    private PackageData packageData;

    public UploadBuildArtifactDelegate(PackageData packageData,
                                       PackageStorage packageStorage) {
        this.packageData = packageData;
        this.packageStorage = packageStorage;
    }

    public UploadBuildArtifactDelegateResponse uploadArtifact(UploadBuildArtifactDelegateRequest request)
            throws PackageNotFoundException, PackageExistsException {
        request.validate();

        String hash = UUID.randomUUID().toString().split("-")[0];

        ArchipelagoBuiltPackage pkg = new ArchipelagoBuiltPackage(request.getPkg(), hash);
        packageData.createBuild(request.getAccountId(), pkg, request.getConfig(), request.getGitCommit());
        String preSignedUrl = packageStorage.generatePreSignedUrl(request.getAccountId(), pkg);
        // TODO: We need to do some checks that the file has been uploaded

        return UploadBuildArtifactDelegateResponse.builder()
                .hash(hash)
                .uploadUrl(preSignedUrl)
                .build();
    }
}
