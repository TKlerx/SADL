package sadl.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RamGobbler extends Thread {

	public double getAvgRam() {
		return meanRam;
	}

	public int getMinRam() {
		return minRam;
	}

	public int getMaxRam() {
		return maxRam;
	}

	Sigar s = new Sigar();

	double meanRam = 0;
	int ramChecks = 0;
	int minRam = Integer.MAX_VALUE;
	int maxRam = Integer.MIN_VALUE;

	static final long PID_NOT_FOUND = -1;
	private static final long ramMonitorWaitingTime = 1000;

	AtomicBoolean shutdown = new AtomicBoolean(false);

	private final Logger logger = LoggerFactory.getLogger(RamGobbler.class);

	public void shutdown() {
		shutdown.set(true);
	}

	long pid = s.getPid();

	@Override
	public void run() {
		try {
			Thread.sleep(ramMonitorWaitingTime);
		} catch (final InterruptedException e) {
			logger.error("Unexpected exception occured.", e);
		}
		while (!shutdown.get()) {
			try {
				try {
					final int currentRamNeeded = (int) s.getProcMem(pid).getResident() / 1000000;
					minRam = Math.min(minRam, currentRamNeeded);
					maxRam = Math.max(maxRam, currentRamNeeded);
					updateMean(currentRamNeeded);

				} catch (final SigarException e) {
					logger.error("Unexpected exception occured.", e);
				}
				Thread.sleep(ramMonitorWaitingTime);
			} catch (final InterruptedException e) {
				logger.error("Unexpected exception occured.", e);
			} finally {
			}
		}
	}

	private void updateMean(int newValue) {
		double sum = meanRam * ramChecks;
		sum += newValue;
		ramChecks++;
		meanRam = sum / ramChecks;
	}

}