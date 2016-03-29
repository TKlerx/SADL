package sadl.run.data_analyzers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;

public class RecursivePlot {
	static String plotExecutable = "/home/timo/bargraph/bargraph.pl";

	public static void main(String[] args) throws IOException, InterruptedException {
		plot(Paths.get(args[0]));
	}

	public static void plot(Path inputDir) throws IOException, InterruptedException {

		final List<Path> plotFiles = new ArrayList<>();

		try {
			Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (!attrs.isDirectory() && file.toString().endsWith(".perf")) {
						plotFiles.add(file);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
					return super.preVisitDirectory(dir, attrs);
				}
			});
		} catch (final IOException e) {
			System.err.println("Unexpected exception occured." + e);
		}
		final ExecutorService es = Executors.newSingleThreadExecutor();
		int i = 1;
		for (final Path p : plotFiles) {
			final String[] command = new String[] { plotExecutable, "-pdf", p.toString() };
			System.out.println(Arrays.toString(command));
			final ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(false);
			Process proc;

			proc = pb.start();
			final Path file = Paths.get(FilenameUtils.removeExtension(p.toString()) + ".pdf");
			Files.deleteIfExists(file);
			es.submit(() -> {
				try (InputStream is = proc.getInputStream();
						OutputStream fw = Files.newOutputStream(file)) {
					int k = 0;
					while ((k = is.read()) != -1) {
						fw.write(k);
					}
					fw.flush();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			final int exitVal = proc.waitFor();
			System.out.println("exitVal for file " + p + " = " + exitVal + " (" + i + "/" + plotFiles.size() + ")");
			i++;
		}
		es.shutdown();
	}
}
