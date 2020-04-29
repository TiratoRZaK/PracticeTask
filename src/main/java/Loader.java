import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.Session;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

public class Loader {
    private static final Logger log = LogManager.getLogger(Loader.class);
    private static ErrorHandler errorHandler;
    private static String TYPE_CONNECTION;
    private static String USER_NAME;
    private static String PASSWORD;
    private static String URL;

    private static Plan PLAN;

    public static void main(String[] args) {
        try {
            parseProperties();
        } catch (LoaderException e) {
            log.error(e);
            return;
        }
        BlockingQueue<Message> blockingQueue = new DelayQueue<Message>();
        MessageProvider provider = new MessageProvider(blockingQueue, PLAN);
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

    private static void parseProperties() throws LoaderException {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream("./src/main/resources/Application.properties")) {
            properties.load(inputStream);
            TYPE_CONNECTION = properties.getProperty("typeConnection");
            URL = properties.getProperty("URL");
            USER_NAME = properties.getProperty("userName");
            PASSWORD = properties.getProperty("password");

            String plan = properties.getProperty("plan");
            PLAN = new Plan(plan);
        } catch (IOException e) {
            throw new LoaderException("Ошибка чтения файла свойств.", e);
        }
    }
}