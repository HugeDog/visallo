package org.visallo.core.bootstrap;

import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.visallo.core.bootstrap.lib.LibLoader;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.ServiceLoaderUtil;

import java.util.Collection;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class InjectHelper {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InjectHelper.class);
    private static Injector injector;

    public static <T> T inject(T o, ModuleMaker moduleMaker, Configuration configuration) {
        ensureInjectorCreated(moduleMaker, configuration);
        inject(o);
        return o;
    }

    public static <T> T inject(T o) {
        if (injector == null) {
            throw new VisalloException("Could not find injector");
        }
        injector.injectMembers(o);
        return o;
    }

    public static Injector getInjector() {
        return injector;
    }

    public static <T> T getInstance(Class<T> clazz, ModuleMaker moduleMaker, Configuration configuration) {
        ensureInjectorCreated(moduleMaker, configuration);
        return injector.getInstance(clazz);
    }

    public static <T> T getInstance(Class<? extends T> clazz) {
        if (injector == null) {
            throw new VisalloException("Could not find injector");
        }
        return injector.getInstance(clazz);
    }

    public static <T> Collection<T> getInjectedServices(Class<T> clazz, Configuration configuration) {
        List<T> workers = toList(ServiceLoaderUtil.load(clazz, configuration));
        for (T worker : workers) {
            inject(worker);
        }
        return workers;
    }

    public static void shutdown() {
        injector = null;
    }

    public static boolean hasInjector() {
        return injector != null;
    }

    public static interface ModuleMaker {
        Module createModule();

        Configuration getConfiguration();
    }

    private static void ensureInjectorCreated(ModuleMaker moduleMaker, Configuration configuration) {
        if (injector == null) {
            LOGGER.info("Loading libs...");
            for (LibLoader libLoader : ServiceLoaderUtil.loadWithoutInjecting(LibLoader.class, configuration)) {
                libLoader.loadLibs(moduleMaker.getConfiguration());
            }
            injector = Guice.createInjector(moduleMaker.createModule(), new ObjectMapperModule());
        }
    }
}
