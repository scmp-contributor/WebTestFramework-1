package com.scmp.framework.manager;

import java.net.URL;
import java.time.Duration;

import com.scmp.framework.utils.ConfigFileKeys;
import com.scmp.framework.context.RunTimeContext;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.scmp.framework.utils.Constants.*;

@Component
public class WebDriverManager {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(WebDriverManager.class);
	private static final ThreadLocal<RemoteWebDriver> remoteWebDriver = new ThreadLocal<>();
	private final RunTimeContext context;

	@Autowired
	public WebDriverManager(RunTimeContext context) {
		this.context = context;
	}

	public static RemoteWebDriver getDriver() {
		return remoteWebDriver.get();
	}

	protected static void setDriver(RemoteWebDriver driver) {
		remoteWebDriver.set(driver);
	}

	public void startDriverInstance(MutableCapabilities browser, Dimension screenDimension)
			throws Exception {
		RemoteWebDriver currentDriverSession;

		// For Execution Mode
		if (!context.isLocalExecutionMode()) {
			currentDriverSession =
					new RemoteWebDriver(new URL(context.getProperty(ConfigFileKeys.HOST_URL)), browser);
		}
		// For Debug Mode, launch local driver
		else {

			if (browser.getBrowserName().equals(Browser.CHROME.browserName())) {
				frameworkLogger.info("Launch local Chrome Browser");
				System.setProperty(
						CHROME_DRIVER_SYSTEM_PROPERTY_NAME,
						context.getGlobalVariables(CHROME_DRIVER_PATH).toString());
				currentDriverSession = new ChromeDriver((ChromeOptions) browser);
			} else {
				frameworkLogger.info("Launch local Firefox Browser");
				System.setProperty(
						FIREFOX_DRIVER_SYSTEM_PROPERTY_NAME,
						context.getGlobalVariables(FIREFOX_DRIVER_PATH).toString());
				currentDriverSession = new FirefoxDriver((FirefoxOptions) browser);
			}
		}

		currentDriverSession.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
		WebDriverManager.setDriver(currentDriverSession);

		// Set screen dimension
		currentDriverSession.manage().window().setSize(screenDimension);
	}

	public void stopWebDriver() {
		if (WebDriverManager.getDriver()!=null) {
			WebDriverManager.getDriver().quit();
		}
	}
}
