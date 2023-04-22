package com.scrapingconsole.gateway.api.configuration;

import io.netty.channel.ChannelDuplexHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomLoggingHandler extends ChannelDuplexHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(CustomLoggingHandler.class);


}
