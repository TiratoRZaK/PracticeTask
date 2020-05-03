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
    private static int COUNT_SENDERS;
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

        List<MessageSender> senders = new ArrayList<>();
        List<Thread> sendThreads = new ArrayList<>();
        for (int i = 0; i < COUNT_SENDERS; i++) {
            MessageSender sender = new MessageSender(
                    TYPE_CONNECTION, URL,
                    "TEST_QUEUE",
                    DeliveryMode.NON_PERSISTENT,
                    Session.AUTO_ACKNOWLEDGE,
                    blockingQueue
            );
            try {
                sender.setConnection();
            } catch (LoaderException e) {
                log.error("Ошибка подключения к MQ.", e);
                return;
            }
            senders.add(sender);
            sendThreads.add(new Thread(sender));
        }
        calculateCountMessages(senders);

        StatisticModule statisticModule = new StatisticModule(senders);
        new Thread(statisticModule).start();

        errorHandler = new ErrorHandler(t1, sendThreads);
        provider.setErrorHandler(errorHandler);
        t1.start();
        for (MessageSender sender : senders) {
            sender.setErrorHandler(errorHandler);
        }
        for (Thread sendThread : sendThreads) {
            sendThread.start();
        }
    }

    private static void calculateCountMessages(List<MessageSender> senders) {
        if (PLAN.getCountMessages() % COUNT_SENDERS == 0) {
            for (MessageSender sender : senders) {
                sender.setCountMessages(PLAN.getCountMessages() / COUNT_SENDERS);
            }
        } else {
            int countMessages = PLAN.getCountMessages();
            int equalShare = PLAN.getCountMessages() / COUNT_SENDERS;
            int modulo = PLAN.getCountMessages() % COUNT_SENDERS;
            int i = 0;
            while (countMessages > equalShare + modulo) {
                countMessages = countMessages - equalShare;
                senders.get(i).setCountMessages(equalShare);
                i++;
            }
            senders.get(i).setCountMessages(equalShare + modulo);
        }
    }

    private static void loadFiles() throws LoaderException {
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
                    FILES.put(name, lines);
                } catch (IOException e) {
                    throw new LoaderException("Ошибка чтения файла: " + file.getPath(), e);
                }
            }
        }
    }

    private static void parseProperties() throws LoaderException {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream("configs/Application.properties")) {
            properties.load(inputStream);
            TYPE_CONNECTION = properties.getProperty("typeConnection");
            URL = properties.getProperty("URL");
            int countSenders = Integer.parseInt(properties.getProperty("countSenders"));
            if (countSenders < 1) {
                throw new LoaderException("Количество потоков отправителей не может быть меньшне единицы.");
            }
            COUNT_SENDERS = countSenders;
            String plan = properties.getProperty("plan");

            PLAN = new Plan(plan);
        } catch (IOException | NumberFormatException e) {
            throw new LoaderException("Ошибка чтения файла свойств.", e);
        }
    }
}