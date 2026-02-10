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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Servlet for downloading large files (bypassing Jetty ResourceHandler's size limits).
 * Streams files directly without buffering the entire file in memory.
 */
public class LargeFileDownloadServlet extends HttpServlet {

    private static final int BUFFER_SIZE = 8192;  // 8KB buffer for streaming
    private String downloadsPath = "/app/downloads";

    @Override
    public void init() throws ServletException {
        super.init();
        // Allow configurable downloads path
        String configuredPath = System.getProperty("pokerweb.downloads.path");
        if (configuredPath != null && !configuredPath.isEmpty()) {
            downloadsPath = configuredPath;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        // Handle directory listing for /downloads/ or /downloads
        if (pathInfo == null || pathInfo.equals("/")) {
            listDirectory(response);
            return;
        }

        // Remove leading slash
        String fileName = pathInfo.substring(1);

        // Security: prevent directory traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
            return;
        }

        File file = new File(downloadsPath, fileName);

        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        // Set content type based on file extension
        String contentType = getServletContext().getMimeType(fileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);

        // Set content length for download progress
        response.setContentLengthLong(file.length());

        // Set filename for download
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // Stream the file
        try (FileInputStream fileInputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        }
    }

    private void listDirectory(HttpServletResponse response) throws IOException {
        File directory = new File(downloadsPath);
        File[] files = directory.listFiles();

        response.setContentType("text/html;charset=UTF-8");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Downloads</title>\n");
        html.append("<style>body{font-family:Arial,sans-serif;margin:40px;}");
        html.append("table{border-collapse:collapse;width:100%;}");
        html.append("th,td{border:1px solid #ddd;padding:8px;text-align:left;}");
        html.append("th{background-color:#4CAF50;color:white;}</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<h1>Downloads</h1>\n");

        if (files != null && files.length > 0) {
            html.append("<table>\n<thead>\n<tr><th>Name</th><th>Size</th></tr>\n</thead>\n<tbody>\n");
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName();
                    long sizeBytes = file.length();
                    String size = formatFileSize(sizeBytes);
                    html.append("<tr><td><a href=\"/downloads/").append(name).append("\">")
                        .append(name).append("</a></td><td>").append(size).append("</td></tr>\n");
                }
            }
            html.append("</tbody>\n</table>\n");
        } else {
            html.append("<p>No files available.</p>\n");
        }

        html.append("</body>\n</html>");
        response.getWriter().write(html.toString());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
