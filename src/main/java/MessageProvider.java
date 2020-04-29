import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/*
 * Он должен генерировать сообщения по шаблону и отправлять в DelayQueue по плану.
 * DelayQueue ему можно передать в конструкторе. Так сейчас работает класс Producer.
 * Только он отправляет сообщения через  ProducerConsumer. Но проще будет сразу в  DelayQueue.
 */
public class MessageProvider implements Runnable {
    private static final Logger log = LogManager.getLogger(MessageProvider.class);
    private BlockingQueue<Message> buffer;
    private Plan plan;
    private ErrorHandler errorHandler;

    public MessageProvider(BlockingQueue<Message> buffer, Plan plan) {
        this.buffer = buffer;
        this.plan = plan;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try {
            MessageGenerator generator = new MessageGenerator();
            long prevTime = 0L;
            log.info("Начало генерации: " + System.currentTimeMillis());
            for (int i = 0; i < plan.countStages(); i++) {
                Plan.Stage currentStage = plan.getStage(i + 1);
                int generatedMessageCount = 0;
                log.info((i + 1) + " этап начало: " + System.currentTimeMillis());
                while (generatedMessageCount < currentStage.getCountMessages()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new LoaderException("Поток завершился извне.");
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
                    log.info("Сформировано: " + message + "Этап: " + currentStage.getNumber());
                    prevTime = message.getStartTime();
                    generatedMessageCount++;
                    try {
                        buffer.put(message);
                    } catch (InterruptedException ex) {
                        throw new LoaderException("Ошибка добавления сообщения в очередь.", ex);
                    }
                }
            }
        } catch ( LoaderException e) {
            errorHandler.closeSender(e);
        }
        log.info("Конец генерации: " + System.currentTimeMillis());
    }
}
