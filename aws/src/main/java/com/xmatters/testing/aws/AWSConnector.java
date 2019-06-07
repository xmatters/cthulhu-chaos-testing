package com.xmatters.testing.aws;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

public class AWSConnector {

    private Ec2Client ec2;

    public AWSConnector() {
        initializeEc2Client();
    }

    synchronized private void initializeEc2Client() {
        if (ec2 == null) {
            ec2 = Ec2Client.create();
        }
    }

    public String[] findInstances(String zoneMask, String instanceNameMask) {
        Pattern zonePattern = Pattern.compile(zoneMask);
        Pattern instancePattern = Pattern.compile(instanceNameMask);

        ArrayList<String> matches = new ArrayList<>();
        forEachInstance(instance -> {
            String zone = instance.placement().availabilityZone();
            if (!zonePattern.matcher(zone).find() || !instancePattern.matcher(getNameTag(instance)).find()) {
                return;
            }

            matches.add(instance.instanceId());
        });

        return matches.toArray(new String[0]);
    }

    private void forEachInstance(Consumer<Instance> logic) {
        String nextToken = null;
        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken).build();
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    logic.accept(instance);
                }
            }

            nextToken = response.nextToken();
        } while (nextToken != null);
    }

    private String getNameTag(Instance instance) {
        Optional<Tag> nameTag = instance.tags().stream()
                .filter(t -> t.key().equals("Name"))
                .findFirst();

        return nameTag.isPresent()
                ? nameTag.get().value()
                : "";
    }

    public void delete(String target) {
        TerminateInstancesRequest request = TerminateInstancesRequest.builder().instanceIds(target).build();
        ec2.terminateInstances(request);
    }

    public void stop(String target) {
        StopInstancesRequest request = StopInstancesRequest.builder().instanceIds(target).build();
        ec2.stopInstances(request);
    }
}
