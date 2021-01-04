package build.archipelago.common.concurrent;

import lombok.Getter;
import lombok.Setter;

public class Wrap<T> {
    @Getter @Setter
    private T value;

    public Wrap(T initialValue) {
        setValue(initialValue);
    }
}
