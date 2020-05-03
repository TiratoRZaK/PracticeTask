import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/*
 * Он должен генерировать сообщения по шаблону и отправлять в DelayQueue по плану.
 * DelayQueue ему можно передать в конструкторе. Так сейчас работает класс Producer.
 * Только он отправляет сообщения через  ProducerConsumer. Но проще будет сразу в  DelayQueue.
 */
public class MessageProvider implements Runnable {
    private static final Logger log = LogManager.getLogger(MessageProvider.class);
    private final BlockingQueue<Message> buffer;
    private final Plan plan;
    private ErrorHandler errorHandler;
    private final Map<String, List<String>> files;

    public MessageProvider(BlockingQueue<Message> buffer, Plan plan, Map<String, List<String>> files) {
        this.buffer = buffer;
        this.plan = plan;
        this.files = files;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try {
            MessageGenerator generator = new MessageGenerator(files);
            long prevTime = 0L;
            log.debug("Начало генерации: " + System.currentTimeMillis());
            for (int i = 0; i < plan.countStages(); i++) {
                Plan.Stage currentStage = plan.getStage(i + 1);
                int generatedMessageCount = 0;
                log.debug((i + 1) + " этап начало: " + System.currentTimeMillis());
                while (generatedMessageCount < currentStage.getCountMessages()) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.error("Поток завершился извне.");
                        break;
                    }
                    long delay = TimeUnit.SECONDS.toMillis(currentStage.getTimeLife()) / currentStage.getCountMessages();
                    Message message;
                    long startTime;
                    if (prevTime == 0L) {
                        startTime = System.currentTimeMillis() + delay;
                    } else {
                        startTime = prevTime + delay;
                    }
                    message = new Message(generator.generate(), startTime);
                    log.debug("Сформировано: " + message + "Этап: " + currentStage.getNumber());
                    prevTime = message.getStartTime();
                    generatedMessageCount++;
                    try {
                        buffer.put(message);
                    } catch (InterruptedException e) {
                        log.error("Ошибка добавления в очередь сообщения: "+message+"\n"
                                + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (LoaderException e) {
            errorHandler.closeSenders(e);
        }
        log.debug("Конец генерации: " + System.currentTimeMillis());
    }
}
