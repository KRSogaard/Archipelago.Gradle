package build.archipelago.versionsetservice.exceptions;

import lombok.Getter;

@Getter
public class VersionSetExistsException extends Exception {

    private String versionSet;

    public VersionSetExistsException(String name) {
        super("Version Set " + name + " already exists");
    }
}
