package com.craftmend.openaudiomc.generic.migrations.migrations;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.authentication.AuthenticationService;
import com.craftmend.openaudiomc.generic.authentication.response.HostDetailsResponse;
import com.craftmend.openaudiomc.generic.core.interfaces.ConfigurationImplementation;
import com.craftmend.openaudiomc.generic.core.storage.enums.StorageKey;
import com.craftmend.openaudiomc.generic.migrations.interfaces.SimpleMigration;

public class AuthHostMigration extends SimpleMigration {

    @Override
    public boolean shouldBeRun() {
        ConfigurationImplementation config = OpenAudioMc.getInstance().getConfiguration();
        return !config.hasStorageKey(StorageKey.AUTH_HOST);
    }

    @Override
    public void execute() {
        migrateFilesFromResources();

        AuthenticationService authenticationService = OpenAudioMc.getInstance().getAuthenticationService();
        ConfigurationImplementation config = OpenAudioMc.getInstance().getConfiguration();
        HostDetailsResponse host = authenticationService.getDriver().getHost();
        if (host.getPreProxyForward() == null) {
            config.setString(StorageKey.AUTH_HOST, host.getIpAddress());
        } else {
            config.setString(StorageKey.AUTH_HOST, host.getPreProxyForward());
        }
        config.saveAll();
    }
}
