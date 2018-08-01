package com.xmatters.testing.kubernetes;

import com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.AutoAdaptableKubernetesClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KubeConnector {

    public static final String CONFIG_KUBE_CONFIG = "kube.config";
    private static final String STATUS_RUNNING = "Running";

    private Path kubeConfigFilePath;

    private KubernetesClient client;

    /* Pod status return healthy even after issuing a delete command on them.  We are making a list of those we affected
       already so that we do not waste operations on them.
     */
    private Set<String> podHistory = new HashSet<>();

    public KubeConnector(ModuleConfiguration config) throws Exception {
        kubeConfigFilePath = Paths.get(config.getConfigValue(CONFIG_KUBE_CONFIG));

        initializeClient();
    }

    private void initializeClient() throws IOException {
        if (client == null) {
            String kubeconfigContents = new String(Files.readAllBytes(kubeConfigFilePath));
            Config config = Config.fromKubeconfig(null, kubeconfigContents, kubeConfigFilePath.toString());
            client = new AutoAdaptableKubernetesClient(config);
        }
    }

    public Pod[] findPods(String namespaceMask, String podNameMask) {
        Pattern namespacePattern = Pattern.compile(namespaceMask);
        Pattern podNamePattern = Pattern.compile(podNameMask);

        return client.pods().list().getItems().stream()
                .filter(pod -> !podHistory.contains(pod.getMetadata().getUid())
                        && pod.getStatus().getPhase().equals(STATUS_RUNNING)
                        && pod.getStatus().getContainerStatuses().stream().allMatch(s -> s.getReady())
                        && namespacePattern.matcher(pod.getMetadata().getNamespace()).find()
                        && podNamePattern.matcher(pod.getMetadata().getName()).find())
                .toArray(Pod[]::new);
    }

    public boolean deletePod(Pod[] pods) {
        boolean success = client.pods().delete(pods);
        if(success) {
            podHistory.addAll(Arrays.stream(pods).map(p -> p.getMetadata().getUid()).collect(Collectors.toSet()));
        }

        return success;
    }
}
