package com.xmatters.testing.slacknotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class HttpPublisher implements NotificationPublisher {

    private URI webhookURI;
    private ObjectMapper mapper = new ObjectMapper();

    public HttpPublisher(URI webhookURI){
        this.webhookURI = webhookURI;
    }

    @Override
    public void publishMessage(SlackMessage message) throws Exception {
        HttpPost request = new HttpPost(webhookURI);
        request.addHeader("content-type", "application/json");
        StringEntity params = new StringEntity(mapper.writeValueAsString(message), StandardCharsets.UTF_8.toString());
        request.setEntity(params);

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);
        if(response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException(response.getStatusLine().toString());
        }
    }
}
