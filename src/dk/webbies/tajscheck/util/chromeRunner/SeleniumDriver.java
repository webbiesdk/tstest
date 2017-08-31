package dk.webbies.tajscheck.util.chromeRunner;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;


/**
 * Created by Erik Krogh Kristensen on 10-11-2015.
 */
public class SeleniumDriver {
    public static String executeScript(File dir, String script, int timeout) throws IOException, HttpException {
        return executeScript(dir, script, timeout, 10 * 1000);
    }
    @SuppressWarnings("Duplicates")
    private static String executeScript(File dir, String script, int timeout, int pageLoadTimeout) throws IOException, HttpException {
        setDriverPath();

        ChromeDriver driver = new ChromeDriver(buldCapabilities());

        try {
            driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
            driver.manage().timeouts().implicitlyWait(pageLoadTimeout, TimeUnit.SECONDS);
            driver.manage().timeouts().setScriptTimeout(pageLoadTimeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Had some error while setting timeouts");
            //noinspection finally
            try {
                Thread.sleep(100);
                driver.quit();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } finally {
                //noinspection ReturnInsideFinallyBlock
                return executeScript(dir, script, timeout, pageLoadTimeout);
            }
        }

        ServerSocket socket = new ServerSocket(0);

        int port = socket.getLocalPort();

        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }

        AtomicBoolean killed = new AtomicBoolean(false);
        AtomicBoolean gotPage = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                Thread.sleep(pageLoadTimeout + 10 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            if (!gotPage.get()) {
                killed.set(true);
                driver.quit();
                try {
                    socket.close();
                } catch (IOException ignored) { }
            }
        }).start();

        AtomicBoolean finished = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                Thread.sleep(timeout + 10 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            if (!finished.get()) {
                killed.set(true);
                driver.quit();
                try {
                    socket.close();
                } catch (IOException ignored) { }
            }
        }).start();

        SimpleMessageReceivingHTTPServer server = startServer(dir, script, socket);

        get("http://127.0.0.1:" + port); // Making a blocking call, that makes sure that my server is up and running.

        try {
            driver.get("http://127.0.0.1:" + port);
            gotPage.set(true);
        } catch (org.openqa.selenium.TimeoutException e) {
            System.err.println("Selenium driver had a timeout loading the index page, trying again!");
            try {
                driver.quit();
                socket.close();
            } catch (Exception ignored) {
            }
            return executeScript(dir, script, timeout, pageLoadTimeout + 10); // continue, try again
        } catch (Exception e) {
            if (killed.get()) {
                return executeScript(dir, script, timeout, pageLoadTimeout + 10); // continue, try again
            } else {
                throw e;
            }
        }


        String message = String.join("\n", server.awaitMessages());

        try {
            driver.quit();
        } catch (Exception e) {
            System.err.println("Had an error while quiting the chrome driver, continueing anyway");
        }

        return message;
    }

    public static int get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        int res = connection.getResponseCode();

        connection.getInputStream().close();

        return res;
    }

    private static SimpleMessageReceivingHTTPServer startServer(File dir, String script, ServerSocket socket) {
        Map<String, String> customContents = new HashMap<>();
        customContents.put("test.js", script);
        try {
            String htmlDriver = IOUtils.toString(SeleniumDriver.class.getResourceAsStream("/driver.html"));
            customContents.put("/", htmlDriver);
            customContents.put("/index.html", htmlDriver);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SimpleMessageReceivingHTTPServer server = new SimpleMessageReceivingHTTPServer(dir, customContents, socket);

        new Thread(server::start).start();

        return server;
    }


    private static void setDriverPath() {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem.contains("Windows")) {
            System.setProperty("webdriver.chrome.driver", "./lib/selenium/chromedriver.exe");
        } else if (operatingSystem.contains("Linux")) {
            System.setProperty("webdriver.chrome.driver", "./lib/selenium/chromedriverLinux64");
        } else if (operatingSystem.contains("Mac")) {
            System.setProperty("webdriver.chrome.driver", "./lib/selenium/chromedriverMac");
        } else {
            throw new RuntimeException("Unknown operating system: " + operatingSystem);
        }
    }

    private static DesiredCapabilities buldCapabilities() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=400,400");

        options.addArguments("headless");
        options.addArguments("no-sandbox");
        options.addArguments("disable-gpu");
        options.addArguments("no-default-browser-check");
        options.addArguments("user-data-dir=./chromedir");

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        LoggingPreferences loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(LogType.BROWSER, Level.ALL);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPreferences);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        return capabilities;
    }
}
