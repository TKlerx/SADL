package sadl.utils;

public class Settings {
	private static boolean debug = false;

	public static boolean isDebug() {
		return Settings.debug;
	}

	public static void setDebug(boolean debug) {
		Settings.debug = debug;
	}

}
