/*
 * MIT License
 *
 * Copyright (c) 2019 Michael Pogrebinsky - Distributed Systems & Cloud Computing with Java
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Introduction to Cloud Computing
 */
public class WebServer {
    private static final String STATUS_ENDPOINT = "/status";
    private static final String HOME_PAGE_ENDPOINT = "/";

    private static final String HTML_PAGE = "index.html";
    private static final String QUOTES_FILE_PATH = "quotes.txt";

    private final int port;
    private HttpServer server;
    private final Random random;
    private final List<String> quoes;


    public WebServer(int port) {
        this.port = port;
        this.random = new Random();
        this.quoes = loadQuotes();
    }

    private List<String> loadQuotes() {
        InputStream quotesStream = getClass().getResourceAsStream(QUOTES_FILE_PATH);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(quotesStream));
        return bufferedReader.lines().collect(Collectors.toUnmodifiableList());
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.createContext(STATUS_ENDPOINT, this::handleStatusCheckRequest);
        server.createContext(HOME_PAGE_ENDPOINT, this::handleHomePageRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        System.out.println(String.format("Started server on port %d ", port));
        server.start();
    }

    private void handleHomePageRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        System.out.println("server received a request for a quote");
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");

        byte[] response = loadHtml(HTML_PAGE);

        sendResponse(response, exchange);
    }

    /**
     * Loads the HTML page to be fetched to the web browser
     *
     * @param htmlFilePath - The relative path to the html file
     * @throws IOException
     */
    private byte[] loadHtml(String htmlFilePath) throws IOException {
        InputStream htmlInputStream = getClass().getResourceAsStream(htmlFilePath);
        if (htmlInputStream == null) {
            return new byte[]{};
        }

        Document document = Jsoup.parse(htmlInputStream, "UTF-8", "");

        String modifiedHtml = populateHtmlDocument(document);
        return modifiedHtml.getBytes("UTF-16");
    }

    /**
     * Fills today's quote and local time in thevHTML document
     *
     * @param document - original HTML document
     */
    private String populateHtmlDocument(Document document) throws UnknownHostException {
        populateQuote(document);
        populateHostName(document);
        return document.toString();
    }

    private void populateQuote(Document document) {
        Element quoteElement = document.selectFirst("#quote");
        String randomQuote = quoes.get(random.nextInt(quoes.size()));
        quoteElement.appendText(randomQuote);
    }

    private void populateHostName(Document document) throws UnknownHostException {
        Element hostElement = document.selectFirst("#host");
        String localhostName = InetAddress.getLocalHost().getHostName();
        hostElement.appendText("Hostname: \"" + localhostName + "\"");
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        System.out.println("Received a health check");
        String responseMessage = "Server is alive\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}
