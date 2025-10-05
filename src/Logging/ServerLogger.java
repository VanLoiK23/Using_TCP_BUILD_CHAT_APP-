package Logging;

import java.util.logging.*;

public class ServerLogger {
    private static final Logger logger = Logger.getLogger("ServerLogger");

    static {
        try {
            FileHandler fh = new FileHandler("server.log", true); // true = append
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
