/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private static final long BYTES_IN_MEGABYTE = 1048576;
	private static final long SHOOTOFF_PROCESS_TIMEOUT = 100; // ms
	
	public static void main(String[] args) throws IOException, InterruptedException {
		String runtimeName = System.getProperty("java.runtime.name");
		String runtimeVersion = System.getProperty("java.runtime.version");
		String jvmBitness = System.getProperty("sun.arch.data.model");
		
		String runtimeData = String.format("Runtime: %s %s %s-bit", runtimeName, runtimeVersion, jvmBitness);
		
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");
		String osPatchLevel = System.getProperty("sun.os.patch.level");
		
		String osData = String.format("OS: %s %s %s, Patch Level: %s", osName, osVersion, osArch, osPatchLevel);
		
		long maxMemory = Runtime.getRuntime().maxMemory() / BYTES_IN_MEGABYTE;
		String memoryAvailableToJVM = String.format("Max amount of memory JVM will use = %d MB", maxMemory);
		
		String diagnosticData = String.format("%s%n%s%n%s%n%n%s", runtimeData, osData, 
				memoryAvailableToJVM, getShootOFFOutput());
		
		logger.error(diagnosticData);
	}

	public static String getShootOFFOutput() throws IOException, InterruptedException {
		Process proc = Runtime.getRuntime().exec("java -jar ShootOFF.jar -d");

		BufferedReader stdInputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stdErrorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		StringBuilder stdOut = new StringBuilder();
		String s = null;
		while ((s = stdInputReader.readLine()) != null) {
		    stdOut.append(s + "\n"); 
		}

		StringBuilder stdErr = new StringBuilder();
		while ((s = stdErrorReader.readLine()) != null) {
			stdErr.append(s + "\n");
		}
		
		Worker worker = new Worker(proc);
		worker.start();
		try {
			worker.join(SHOOTOFF_PROCESS_TIMEOUT);
		} catch (InterruptedException ex) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			throw ex;
		} finally {
			proc.destroy();
		}
		
		// Null exit code means ran until timeout
		return String.format("ShootOFF Output:%n%nexit code: %d%n%n"
				+ "stdout:%n---%n%s%n%n"
				+ "stderr:%n---%n%s", worker.getExitCode(), stdOut.toString(), stdErr.toString());
	}
	
	private static class Worker extends Thread {
		private final Process process;
		private Integer exitCode;

		private Worker(Process process) {
			this.process = process;
		}

		public void run() {
			try {
				exitCode = process.waitFor();
			} catch (InterruptedException ignore) {
				return;
			}
		}
		
		public Integer getExitCode() {
			return exitCode;
		}
	}
}