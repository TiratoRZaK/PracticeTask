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
    private final Logger log = LogManager.getLogger(Loader.class);
    private int countSenders;
    private ErrorHandler errorHandler;
    private String typeConnection;
    private String url;
    private final Map<String, List<String>> files = new HashMap<>();
    private Plan plan;

    public static void main(String[] args) {
        Loader loader = new Loader();
        loader.startLoader();
    }

    public void startLoader() {
        try {
            parseProperties();
            loadFiles();
        } catch (LoaderException e) {
            log.error(e);
            return;
        }
        BlockingQueue<Message> blockingQueue = new DelayQueue<>();
        MessageProvider provider = new MessageProvider(blockingQueue, plan, files);
        Thread t1 = new Thread(provider);

        StatisticsWriter statisticsWriter = new StatisticsWriter(plan);

        List<MessageSender> senders = new ArrayList<>();
        List<Thread> sendThreads = new ArrayList<>();

        for (int i = 0; i < countSenders; i++) {
            MessageSender sender = new MessageSender(
                    typeConnection, url,
                    "TEST_QUEUE",
                    DeliveryMode.NON_PERSISTENT,
                    Session.AUTO_ACKNOWLEDGE,
                    blockingQueue,
                    statisticsWriter
            );
            try {
                sender.setConnection();
            } catch (LoaderException e) {
                log.error("Ошибка подключения к MQ.", e);
                break;
            }
            senders.add(sender);
            sendThreads.add(new Thread(sender));
        }

        errorHandler = new ErrorHandler(t1, sendThreads);
        provider.setErrorHandler(errorHandler);
        t1.start();
        for (MessageSender sender : senders) {
            sender.setErrorHandler(errorHandler);
        }
        for (Thread sendThread : sendThreads) {
            sendThread.start();
        }
        new Thread(statisticsWriter).start();
    }

    private void loadFiles() throws LoaderException {
        File[] files = new File("configs/files/").listFiles();
        if (files != null) {
            for (File file : files) {
                String name = FilenameUtils.getBaseName(file.getName());
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                    this.files.put(name, lines);
                } catch (IOException e) {
                    throw new LoaderException("Ошибка чтения файла: " + file.getPath(), e);
                }
            }
        }
    }

    private void parseProperties() throws LoaderException   {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream("configs/Application.properties")) {
            properties.load(inputStream);
            typeConnection = properties.getProperty("typeConnection");
            url = properties.getProperty("URL");
            int countSenders = Integer.parseInt(properties.getProperty("countSenders"));
            if (countSenders < 1) {
                throw new LoaderException("Количество потоков отправителей не может быть меньшне единицы.");
            }
            this.countSenders = countSenders;
            String plan = properties.getProperty("plan");

            this.plan = new Plan(plan);
        } catch (IOException | NumberFormatException e) {
            throw new LoaderException("Ошибка чтения файла свойств.", e);
        }
    }
}