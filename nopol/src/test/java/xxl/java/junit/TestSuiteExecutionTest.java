package xxl.java.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import fr.inria.lille.repair.common.config.Config;
import org.junit.Test;
import org.junit.runner.Result;

import xxl.java.library.FileLibrary;

public class TestSuiteExecutionTest {

	@Test
	public void runSuite() {
		Result result = TestSuiteExecution.runCasesIn(new String[]{ sampleTestClass() }, classLoaderWithTestClass(), new Config());
		assertFalse(result.wasSuccessful());
		assertTrue(3 == result.getRunCount());
		assertTrue(1 == result.getFailureCount());
	}
	
	@Test
	public void runSingleTest() {
		TestCase testCase = TestCase.from(sampleTestClass(), "joinTrue", 1);
		Result result = TestSuiteExecution.runTestCase(testCase, classLoaderWithTestClass(), new Config());
		assertTrue(result.wasSuccessful());
		assertEquals(1, result.getRunCount());
		assertEquals(0, result.getFailureCount());
	}
	
	@Test
	public void runSuiteWithTestListener() {
		TestCasesListener listener = new TestCasesListener();
		TestSuiteExecution.runCasesIn(new String[]{ sampleTestClass() }, classLoaderWithTestClass(), listener, new Config());
		assertEquals(3, listener.allTests().size());
		assertEquals(2, listener.successfulTests().size());
		assertEquals(1, listener.failedTests().size());
	}
	
	@Test
	public void doNotUseSameTestNameTwice() {
		TestCasesListener listener = new TestCasesListener();
		TestSuiteExecution.runCasesIn(new String[]{ sampleTestClass(), sampleTestClass() }, classLoaderWithTestClass(), listener, new Config());
		assertEquals(6, listener.allTests().size());
		assertEquals(4, listener.successfulTests().size());
		assertEquals(2, listener.failedTests().size());
	}
	
	@Test
	public void compoundResultForMultipleTestCases() {
		TestCasesListener listener = new TestCasesListener();
		TestSuiteExecution.runCasesIn(new String[]{ sampleTestClass() }, classLoaderWithTestClass(), listener, new Config());
		Collection<TestCase> failedTests = listener.failedTests();
		assertFalse(failedTests.isEmpty());
		Collection<TestCase> successfulTests = listener.successfulTests();
		assertFalse(successfulTests.isEmpty());
		CompoundResult compound;
		
		compound = TestSuiteExecution.runTestCases(failedTests, classLoaderWithTestClass(), new Config());
		assertFalse(compound.wasSuccessful());
		assertTrue(failedTests.size() == compound.getFailureCount());
		assertTrue(failedTests.size() == compound.getRunCount());
		assertTrue(0 == compound.getIgnoreCount());
		assertTrue(compound.successes().isEmpty());
		
		compound = TestSuiteExecution.runTestCases(successfulTests, classLoaderWithTestClass(), new Config());
		assertTrue(compound.wasSuccessful());
		assertTrue(0 == compound.getFailureCount());
		assertTrue(successfulTests.size() == compound.getRunCount());
		assertTrue(0 == compound.getIgnoreCount());
		assertTrue(compound.failures().isEmpty());
	}
	
	@Test
	public void runJUnit3Tests() {
		TestCasesListener listener = new TestCasesListener();
		TestSuiteExecution.runCasesIn(new String[]{ sampleTestCase() }, classLoaderWithTestCase(), listener, new Config());
		Collection<TestCase> cases = listener.allTests();
		assertEquals(3, cases.size());
	}
	
	private ClassLoader classLoaderWithTestClass() {
		URL resource = FileLibrary.resource("/sampleTestClass/TestClass.jar");
		return new URLClassLoader(new URL[] {resource});
	}
	
	private ClassLoader classLoaderWithTestCase() {
		URL resource = FileLibrary.resource("/sampleTestCase/SampleTestCase.jar");
		return new URLClassLoader(new URL[] {resource});
	}
	
	private String sampleTestClass() {
		return "xxl.java.junit.sample.TestClass";
	}
	
	private String sampleTestCase() {
		return "xxl.java.junit.SampleTestCase";
	}
}
