package build.archipelago.common.concurrent;

import lombok.*;

public class Wrap<T> {
    @Getter @Setter
    private T value;

    public Wrap(T initialValue) {
        setValue(initialValue);
    }
}
