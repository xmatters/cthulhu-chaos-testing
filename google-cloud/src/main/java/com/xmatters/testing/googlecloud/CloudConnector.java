package com.xmatters.testing.googlecloud;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Zone;
import com.google.common.base.Strings;
import com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CloudConnector {

    private static final String CONFIG_APP_NAME = "application.name";
    private static final String CONFIG_CREDENTIAL_PATH = "gcp.account.json";
    private static final String CONFIG_PROJECT_ID = "gcp.project";
    private static final String STATUS_RUNNING = "RUNNING";

    private String applicationName;
    private String projectId;
    private String credentialFilePath;

    private Integer singletonLock = 0;
    private Compute client;

    public CloudConnector(ModuleConfiguration config) throws Exception {
        applicationName = config.getConfigValue(CONFIG_APP_NAME);
        credentialFilePath = config.getConfigValue(CONFIG_CREDENTIAL_PATH);
        projectId = config.getConfigValue(CONFIG_PROJECT_ID);

        initializeComputeClient();
    }

    private void initializeComputeClient() throws Exception {
        if (client == null) {
            synchronized (singletonLock) {
                if (client == null) {
                    File credentialFile = new File(credentialFilePath);
                    InputStream credentialStream = new FileInputStream(credentialFile);
                    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                    GoogleCredential credential = GoogleCredential.fromStream(credentialStream);

                    if (credential.createScopedRequired()) {
                        List<String> scopes = new ArrayList<>();
                        scopes.add(ComputeScopes.COMPUTE);
                        credential = credential.createScoped(scopes);
                    }

                    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                    Compute.Builder builder = new Compute.Builder(httpTransport, jsonFactory, credential);
                    builder.setApplicationName(applicationName);
                    client = builder.build();
                }
            }
        }
    }

    public Instance[] findInstances(String zoneMask, String instanceMask) throws Exception {
        if (Strings.isNullOrEmpty(projectId)) {
            throw new ConfigurationException(CONFIG_PROJECT_ID + " is required.");
        }

        Pattern zonePattern = Pattern.compile(zoneMask);
        Pattern instancePattern = Pattern.compile(instanceMask);

        List<Instance> targets = new ArrayList<>();
        for (Zone zone : client.zones().list(projectId).execute().getItems()) {
            if (!zonePattern.matcher(zone.getName()).find()) {
                continue;
            }

            InstanceList list = client.instances().list(projectId, zone.getName()).execute();
            List<Instance> candidates = list.getItems();
            if (candidates == null || candidates.size() == 0) {
                continue;
            }

            List<Instance> matches = candidates.stream()
                    .filter(i -> i.getStatus().equals(STATUS_RUNNING) && instancePattern.matcher(i.getName()).find())
                    .collect(Collectors.toList());
            targets.addAll(matches);
        }

        return targets.toArray(new Instance[0]);
    }

    public Operation deleteInstance(Instance instance) throws IOException {
        InstanceIdentity i = InstanceIdentity.extract(instance);
        Compute.Instances.Delete deleteCommand = client.instances().delete(i.getProject(), i.getZone(), i.getName());
        return deleteCommand.execute();
    }

    public Operation resetInstance(Instance instance) throws IOException {
        InstanceIdentity i = InstanceIdentity.extract(instance);
        Compute.Instances.Reset resetCommand = client.instances().reset(i.getProject(), i.getZone(), i.getName());
        return resetCommand.execute();
    }

    public Operation stopInstance(Instance instance) throws IOException {
        InstanceIdentity i = InstanceIdentity.extract(instance);
        Compute.Instances.Stop stopCommand = client.instances().stop(i.getProject(), i.getZone(), i.getName());
        return stopCommand.execute();
    }
}


