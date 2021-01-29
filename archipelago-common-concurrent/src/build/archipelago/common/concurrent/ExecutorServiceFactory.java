package build.archipelago.common.concurrent;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceFactory {
    ExecutorService create();
}
