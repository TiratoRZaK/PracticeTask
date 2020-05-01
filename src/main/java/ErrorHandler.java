import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ErrorHandler {
    private static final Logger log = LogManager.getLogger(ErrorHandler.class);
    private final Thread generator;
    private final Thread sender;

    public ErrorHandler(Thread generator, Thread sender) {
        this.generator = generator;
        this.sender = sender;
    }

    public void closeGenerator(Throwable cause) {
        log.error("Вынужденная остановка генерации сообщений.", cause);
        generator.interrupt();
    }

    public void closeSender(Throwable cause) {
        log.error("Вынужденная остановка отправки сообщений.", cause);
        sender.interrupt();
    }
}
