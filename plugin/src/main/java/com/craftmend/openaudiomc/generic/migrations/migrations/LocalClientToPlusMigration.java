package com.craftmend.openaudiomc.generic.migrations.migrations;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.interfaces.OAConfiguration;
import com.craftmend.openaudiomc.generic.loggin.OpenAudioLogger;
import com.craftmend.openaudiomc.generic.migrations.interfaces.SimpleMigration;
import com.craftmend.openaudiomc.generic.migrations.wrapper.UploadSettingsWrapper;
import com.craftmend.openaudiomc.generic.plus.response.ClientSettingsResponse;
import com.craftmend.openaudiomc.generic.rest.RestRequest;
import com.craftmend.openaudiomc.generic.storage.enums.StorageKey;
import com.craftmend.openaudiomc.generic.storage.objects.ClientSettings;

import java.util.HashMap;
import java.util.Map;

public class LocalClientToPlusMigration implements SimpleMigration {

    @Override
    public boolean shouldBeRun() {
        // Ignore deprecation warnings, that's the point
        ClientSettings settings = new ClientSettings().load();
        return !settings.equals(new ClientSettings());
    }

    @Override
    public void execute() {
        ClientSettings settings = new ClientSettings().load();
        ClientSettingsResponse clientSettingsResponse = new ClientSettingsResponse();
        // apply key so we can force it down its throat
        String privateKey = OpenAudioMc.getInstance().getAuthenticationService().getServerKeySet().getPrivateKey().getValue();

        OpenAudioLogger.toConsole("Found old legacy client settings, migrating them to OpenAudioMc+");
        if (!settings.getBackground().equals("default") && !settings.getBackground().startsWith("<un"))
            clientSettingsResponse.setBackgroundImage(settings.getBackground());

        if (!settings.getErrorMessage().equals("default") && !settings.getErrorMessage().startsWith("<un"))
            clientSettingsResponse.setClientErrorMessage(settings.getErrorMessage());

        if (!settings.getWelcomeMessage().equals("default") && !settings.getWelcomeMessage().startsWith("<un"))
            clientSettingsResponse.setClientWelcomeMessage(settings.getWelcomeMessage());

        if (!settings.getTitle().equals("default") && !settings.getTitle().startsWith("<un"))
            clientSettingsResponse.setTitle(settings.getTitle());

        // check for start sound
        OAConfiguration OAConfiguration = OpenAudioMc.getInstance().getOAConfiguration();
        String startSound = OAConfiguration.getString(StorageKey.SETTINGS_CLIENT_START_SOUND);
        if (startSound != null && !startSound.equals("none") && !startSound.startsWith("<un"))
            clientSettingsResponse.setStartSound(startSound);

        RestRequest upload = new RestRequest("/api/v1/plus/settings");
        upload.setBody(OpenAudioMc.getGson().toJson(new UploadSettingsWrapper(privateKey, clientSettingsResponse)));
        upload.executeSync();

        // settings that should be moved over
        Map<StorageKey, Object> oldValues = new HashMap<>();
        for (StorageKey value : StorageKey.values()) {
            if (!value.isDeprecated()) {
                oldValues.put(value, OpenAudioMc.getInstance().getOAConfiguration().get(value));
            }
        }

        // force reload conf
        OpenAudioMc.getInstance().getOAConfiguration().saveAllhard();

        // force update values
        for (Map.Entry<StorageKey, Object> entry : oldValues.entrySet()) {
            StorageKey key = entry.getKey();
            Object value = entry.getValue();
            OpenAudioLogger.toConsole("Migrating " + key.name() + " value " + value.toString());
            OpenAudioMc.getInstance().getOAConfiguration().set(key, value);
        }

        // soft save to reflect the old values and write them to the new file
        OpenAudioMc.getInstance().getOAConfiguration().saveAll();
    }
}