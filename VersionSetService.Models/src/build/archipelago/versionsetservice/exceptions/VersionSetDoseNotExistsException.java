package build.archipelago.versionsetservice.exceptions;

import build.archipelago.common.exceptions.ArchipelagoException;
import lombok.Getter;

@Getter
public class VersionSetDoseNotExistsException extends ArchipelagoException {
    private String versionSet;
    private String revision;

    public VersionSetDoseNotExistsException(String name) {
        super(getMessage(name));
        this.versionSet = name;
    }

    public VersionSetDoseNotExistsException(String name, Exception exp) {
        super(getMessage(name), exp);
        this.versionSet = name;
    }

    public VersionSetDoseNotExistsException(String name, String revision) {
        super(getMessage(name, revision));
        this.versionSet = name;
        this.revision = revision;
    }

    public VersionSetDoseNotExistsException(String name, String revision, Exception exp) {
        super(getMessage(name, revision), exp);
        this.revision = revision;
    }

    private static String getMessage(String name) {
        return "Version Set " + name + " dose not exist";
    }

    private static String getMessage(String name, String revision) {
        return getMessage(name + "#" + revision);
    }
}
