package hu.blackbelt.karaf.jasypt;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@Slf4j
public class Activator implements BundleActivator {

    private static final String ENCRYPTION_ALGORITHM = "PBEWithSHA1AndDESEDE";
    private static final String ENCRYPTION_PASSWORD_ENV_NAME = "ENCRYPTION_PASSWORD";

    private ServiceRegistration<StringEncryptor> stringEncryptor;

    @Override
    public void start(final BundleContext context) {
        final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        final EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
        config.setAlgorithm(ENCRYPTION_ALGORITHM);
        config.setPasswordEnvName(ENCRYPTION_PASSWORD_ENV_NAME);
        encryptor.setConfig(config);

        stringEncryptor = context.registerService(StringEncryptor.class, encryptor, null);
    }

    @Override
    public void stop(final BundleContext context) {
        try {
            if (stringEncryptor != null) {
                stringEncryptor.unregister();
            }
        } finally {
            stringEncryptor = null;
        }
    }
}
