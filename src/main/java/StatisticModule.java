import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class StatisticModule extends Thread{
    private static final Logger logger = LogManager.getLogger(StatisticModule.class);
    private List<MessageSender> senders;

    public StatisticModule(List<MessageSender> senders) {
        this.senders = senders;
    }

    @Override
    public void run() {
        int countEndedThreads = 0;
        int prevCountSentMessages = 0;
        while(countEndedThreads != senders.size()) {
            int countSentMessages = 0;
            for (MessageSender sender : senders) {
                countSentMessages += sender.getCountSentMessages();
                if(sender.isCompletedSending()){
                    countEndedThreads++;
                }
            }
            logger.info("\nКоличество отправленных сообщений = " + countSentMessages +
                        "\nКоличество отправленных сообщений за последнюю секунду = " + ( countSentMessages - prevCountSentMessages ));
            prevCountSentMessages = countSentMessages;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Ошибка в работе модуля статистики.", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
