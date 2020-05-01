import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.Session;
import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

public class Loader {
    private static final Logger log = LogManager.getLogger(Loader.class);
    private static ErrorHandler errorHandler;
    private static String TYPE_CONNECTION;
    private static String URL;
    private static final Map<String, List<String>> FILES = new HashMap<>();

    private static Plan PLAN;

    public static void main(String[] args) {
        try {
            parseProperties();
            loadFiles();
        } catch (LoaderException e) {
            log.error(e);
            return;
        }
        BlockingQueue<Message> blockingQueue = new DelayQueue<>();
        MessageProvider provider = new MessageProvider(blockingQueue, PLAN, FILES);
        Thread t1 = new Thread(provider);
        MessageSender sender = new MessageSender(
                TYPE_CONNECTION, URL,
                "TEST_QUEUE",
                DeliveryMode.NON_PERSISTENT,
                Session.AUTO_ACKNOWLEDGE,
                blockingQueue,
                PLAN
        );
        try {
            sender.setConnection();
        } catch (LoaderException e) {
            log.error("Ошибка подключения к MQ.", e);
            return;
        }
        Thread t2 = new Thread(sender);
        errorHandler = new ErrorHandler(t1, t2);
        provider.setErrorHandler(errorHandler);
        sender.setErrorHandler(errorHandler);
        t1.start();
        t2.start();

    }

    private static void loadFiles() throws LoaderException {
        File[] files = new File("./configs/files/").listFiles();
        if (files != null) {
            for (File file : files) {
                String name = FilenameUtils.getBaseName(file.getName());
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                    FILES.put(name, lines);
                } catch (IOException e) {
                    throw new LoaderException("Ошибка чтения файла: " + file.getPath(), e);
                }
            }
        }
    }

    private static void parseProperties() throws LoaderException {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream("./configs/Application.properties")) {
            properties.load(inputStream);
            TYPE_CONNECTION = properties.getProperty("typeConnection");
            URL = properties.getProperty("URL");

            String plan = properties.getProperty("plan");
            PLAN = new Plan(plan);
        } catch (IOException e) {
            throw new LoaderException("Ошибка чтения файла свойств.", e);
        }
    }
}