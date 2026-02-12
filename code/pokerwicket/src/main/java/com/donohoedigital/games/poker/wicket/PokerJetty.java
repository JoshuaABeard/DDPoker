/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.wicket;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.LoggingConfig;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Class for running Poker Wicket website via Jetty (mainly for use locally).
 */
public class PokerJetty {

    public static void main(String[] args) {

        // initialize logging first
        LoggingConfig loggingConfig = new LoggingConfig("poker", ApplicationType.WEBAPP);
        loggingConfig.init();
        Logger logger = LogManager.getLogger(PokerJetty.class);

        Server server = getServer();

        try {
            logger.info("========================================");
            logger.info("DD Poker Web Server Starting");
            logger.info("Version: {}", PokerConstants.VERSION);
            logger.info("========================================");
            logger.info(">>> STARTING EMBEDDED JETTY SERVER");
            server.start();

            // In Docker (no TTY), just wait for the server to stop via signal
            // Locally, press any key to stop
            if (System.getProperty("pokerweb.daemon", "false").equals("true")) {
                logger.info(">>> Running in daemon mode. Send SIGTERM to stop.");
                server.join();
            } else {
                logger.info(">>> PRESS ANY KEY TO STOP");
                while (System.in.available() == 0) {
                    // noinspection BusyWait
                    Thread.sleep(500);
                }
                server.stop();
                server.join();
            }
        } catch (Exception e) {
            // need to re-fetch logger since logging is re-initialized
            logger = LogManager.getLogger(PokerJetty.class);
            logger.error(Utils.formatExceptionText(e));
            System.exit(-1);
        }
    }

    private static Server getServer() {
        int port = Integer.parseInt(System.getProperty("pokerweb.port", "8080"));

        // Configure HTTP settings for large file downloads (MSI files are ~100MB)
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(32768); // 32KB buffer for chunking
        httpConfig.setOutputAggregationSize(8192); // Aggregate up to 8KB before flushing
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(true);
        httpConfig.setHttpCompliance(HttpCompliance.RFC7230);

        // Create server with configured connector
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        connector.setIdleTimeout(300000); // 5 minute timeout for large downloads
        server.addConnector(connector);

        // Setup webapp context
        // Note: /downloads is handled by LargeFileDownloadServlet registered in web.xml
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setServer(server);
        webAppContext.setContextPath("/");
        // Configurable webapp path: defaults to repo-relative path for local dev
        String warPath = System.getProperty("pokerweb.war.path", "code/pokerwicket/src/main/webapp");
        webAppContext.setWar(warPath);
        // Allow large file uploads (MSI files are ~100MB)
        webAppContext.setMaxFormContentSize(150 * 1024 * 1024); // 150MB
        webAppContext.setMaxFormKeys(10000);

        server.setHandler(webAppContext);
        return server;
    }
}
