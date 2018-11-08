package org.aksw.limes.core.controller;

public class RunConfiguration {

	private final int maxIterations;
	private final String runConfigurationFile;
	private final String evaluationLoggingFile;
	
	public RunConfiguration(int maxIterations, String runConfigurationFile, String evaluationLoggingFile) {
		this.maxIterations = maxIterations;
		this.runConfigurationFile = runConfigurationFile;
		this.evaluationLoggingFile = evaluationLoggingFile;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public String getRunConfigurationFile() {
		return runConfigurationFile;
	}

	public String getEvaluationLoggingFile() {
		return evaluationLoggingFile;
	}
	
	
}
