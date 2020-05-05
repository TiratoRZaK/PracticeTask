import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class StatisticsWriter extends Thread {
    private static final Logger logger = LogManager.getLogger(StatisticsWriter.class);
    private final AtomicInteger countSentMessages = new AtomicInteger(0);
    private final Plan plan;
    private volatile boolean completedSent = false;

    public StatisticsWriter(Plan plan) {
        this.plan = plan;
    }

    public void addSentMessage() {
        if (countSentMessages.incrementAndGet() == plan.getCountMessages()) {
            completedSent = true;
        }
    }

    public boolean isCompletedSent() {
        return completedSent;
    }

    @Override
    public void run() {
        int prevCountSentMessages = 0;
        do{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Ошибка в работе модуля статистики.", e);
                Thread.currentThread().interrupt();
                break;
            }
            logger.info("\nКоличество отправленных сообщений = " + countSentMessages.get() +
                    "\nКоличество отправленных сообщений за последнюю секунду = " + (countSentMessages.get() - prevCountSentMessages));
            prevCountSentMessages = countSentMessages.get();

        } while (countSentMessages.get() < plan.getCountMessages());
    }
}
