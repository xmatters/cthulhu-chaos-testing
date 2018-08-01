package com.xmatters.testing.slacknotifications;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackMessage {

    @Getter @Setter
    private String channel;

    @Getter @Setter
    private String username;

    @Getter @Setter
    @JsonProperty(value = "icon_emoji")
    private String iconEmoji;

    @Getter @Setter
    private String text;
}
