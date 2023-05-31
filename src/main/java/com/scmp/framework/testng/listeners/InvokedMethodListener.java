package com.scmp.framework.testng.listeners;

import java.lang.reflect.Method;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testng.model.TestInfo;
import com.scmp.framework.manager.WebDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.*;

import com.scmp.framework.report.ReportManager;

import static com.scmp.framework.utils.Constants.TEST_INFO_OBJECT;


public final class InvokedMethodListener implements IInvokedMethodListener {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(InvokedMethodListener.class);

	private final WebDriverManager driverManager;
	private final RunTimeContext runTimeContext;
	private final ReportManager reportManager;

	public InvokedMethodListener() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		driverManager = context.getBean(WebDriverManager.class);
		runTimeContext = context.getBean(RunTimeContext.class);
		reportManager = context.getBean(ReportManager.class);
	}

	/**
	 * Setup reporter in report manager
	 *
	 * @param testInfo test metadata for the test case
	 */
	private void setupReporterForTest(TestInfo testInfo) {
		try {
			// Create test node for test class in test report
			reportManager.setupReportForTestSet(testInfo);
			reportManager.setTestResult(testInfo.getTestResult());

			// Create test case in test report
			reportManager.setTestInfo(testInfo);
			reportManager.setSetupStatus(true);
		} catch (Exception e) {
			frameworkLogger.error("Ops!", e);
		}
	}

	/**
	 * Setup web driver for current test
	 *
	 * @param testInfo test metadata for the test case
	 * @throws Exception exception for starting driver instance
	 */
	private void setupDriverForTest(TestInfo testInfo) throws Exception {

		MutableCapabilities browserOptions = testInfo.getBrowserOption();
		Dimension deviceDimension = testInfo.getDeviceDimension();

		try {
			// Setup web driver
			driverManager.startDriverInstance(browserOptions, deviceDimension);
		} catch (Exception ex1) {
			if (!runTimeContext.isLocalExecutionMode()) {
				driverManager.stopWebDriver();
				// Wait 30 seconds and retry driver setup
				Thread.sleep(30000);

				// Setup web driver
				driverManager.startDriverInstance(browserOptions, deviceDimension);
			} else {
				throw ex1;
			}
		}
	}

	/**
	 * Before each method invocation
	 * Initialize Web Driver and Report Manager
	 */
	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {

		// Clear all runtime variables
		runTimeContext.clearRunTimeVariables();

		TestInfo testInfo = new TestInfo(method, testResult, runTimeContext);
		// Save TestInfo to runtime memory
		runTimeContext.setTestLevelVariables(TEST_INFO_OBJECT, testInfo);

		// Skip beforeInvocation if current method is not with Annotation Test, or
		// Current Test need to be skipped
		if (!testInfo.isTestMethod() || testInfo.isSkippedTest()) {
			throw new SkipException("Skipped Test - " + testInfo.getTestName());
		}

		frameworkLogger.info("Start running test [" + testInfo.getMethodName() + "]");
		try {
			if (testInfo.needLaunchBrowser()) {
				setupDriverForTest(testInfo);
			}
			setupReporterForTest(testInfo);
		} catch (Exception ex) {
			ex.printStackTrace();
			reportManager.setSetupStatus(false);
			Assert.fail("Fails to setup test driver.");
		}
	}

	/**
	 * After each method invocation
	 * Update test result to report manager and stop Web Driver
	 */
	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

		TestInfo testInfo = (TestInfo) runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		// Skip beforeInvocation if current method is not with Annotation Test, or
		// Current Test need to be skipped
		if (!testInfo.isTestMethod() || testInfo.isSkippedTest()) {
			return;
		}

		Method refMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
		String methodName = refMethod.getName();

		frameworkLogger.info("Completed running test [" + methodName + "]");

		// If fails to set up test
		if (!reportManager.getSetupStatus()) {
			if (testInfo.needLaunchBrowser()) {
				driverManager.stopWebDriver();
			}

			return;
		}

		try {
			reportManager.endLogTestResults(testResult);
			// Clear all runtime variables
			runTimeContext.clearRunTimeVariables();

			// Stop driver
			if (testInfo.needLaunchBrowser()) {
				driverManager.stopWebDriver();
			}
		} catch (Exception e) {
			frameworkLogger.error("Ops!", e);
		}
	}
}
