import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ErrorHandler {
    private static final Logger log = LogManager.getLogger(ErrorHandler.class);
    private final Thread generator;
    private final List<Thread> senders;

    public ErrorHandler(Thread generator, List<Thread> senders) {
        this.generator = generator;
        this.senders = senders;
    }

    public void closeGenerator(Throwable cause) {
        log.error("Вынужденная остановка генерации сообщений.", cause);
        generator.interrupt();
    }

    public void closeSenders(Throwable cause) {
        log.error("Вынужденная остановка отправки сообщений.", cause);
        for (Thread sender : senders) {
            sender.interrupt();
        }
    }
}
