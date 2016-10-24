package hu.blackbelt.karaf.jasypt.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jasypt.encryption.StringEncryptor;

@Command(scope = "jasypt", name = "decrypt", description = "Decrypt String value.")
@Service
public class Decrypt implements Action {

    @Argument(index = 0, name = "text", description = "Text to decrypt", required = true, multiValued = false)
    private String text;

    @Reference
    private StringEncryptor encryptor;

    @Override
    public Object execute() {
        System.out.println("Decrypted value is: " + encryptor.decrypt(text));

        return null;
    }
}
