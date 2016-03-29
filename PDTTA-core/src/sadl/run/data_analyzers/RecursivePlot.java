package sadl.run.data_analyzers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class RecursivePlot {
	static String plotExecutable = "";

	public static void main(String[] args) throws IOException, InterruptedException {
		plot(Paths.get(args[0]));
	}

	public static void plot(Path inputDir) throws IOException, InterruptedException {

		final List<Path> plotFiles = new ArrayList<>();

		try {
			Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (!attrs.isDirectory() && file.toString().endsWith(".prep")) {
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
		for (final Path p : plotFiles) {
			final String[] command = new String[] { plotExecutable, p.toString(), "-pdf", ">", p.toString() + ".pdf" };
			final ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process proc;

			proc = pb.start();
			final int exitVal = proc.waitFor();
		}
	}
}
