package build.archipelago.common.exceptions;

public class PackageVersionConflictException extends ArchipelagoException {

    private String name;
    private String version1;
    private String version2;

    public PackageVersionConflictException(String name, String version1, String version2) {
        super("A major version conflict was detected for package " + name + " ["+version1+", "+version2+"]");
        this.name = name;
        this.version1 = version1;
        this.version2 = version2;
    }
}
