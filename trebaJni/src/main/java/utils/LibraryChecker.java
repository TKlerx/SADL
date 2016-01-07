package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LibraryChecker {

	public static boolean isInstalled(final String libName) {
		final String[][] arguments = { { "ldconfig", "-p" }, { "whereis", libName } };
		for (final String[] args : arguments) {
			try {
				final StringBuilder sb = getProcessOutput(args);
				if (sb.indexOf(libName + ".") > 0) {
					return true;
				}
			} catch (final IOException e) {
				if (!e.getMessage().contains("Cannot run")) {
					System.err.println("Found strange exception:");
					e.printStackTrace();
					return false;
				}
			}
		}
		System.err.println("Did not find library " + libName);
		return false;
	}

	protected static StringBuilder getProcessOutput(final String[] args) throws IOException {
		Process p1 = null;
		final ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream(true);
		p1 = pb.start();

		final InputStream is = p1.getInputStream();
		final InputStreamReader isr = new InputStreamReader(is);
		final BufferedReader br = new BufferedReader(isr);
		String line = null;
		final StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb;
	}

	public static boolean trebaDepsInstalled() {
		final String[] deps = { "gslcblas", "gsl", "pthread" };
		for (final String lib : deps) {
			if (!isInstalled(lib)) {
				return false;
			}
		}
		return true;
	}

}