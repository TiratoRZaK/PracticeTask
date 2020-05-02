import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import java.util.concurrent.BlockingQueue;

/*
 * Его задачи:
 *   подключиться к MQ,
 *   читать сообщения из DelayQueue,
 *   отправлять в MQ,
 *   закрыть соединение, когда все сообщения будут отправлены.
 * */
public class MessageSender implements Runnable {
    private static final Logger log = LogManager.getLogger(MessageSender.class);
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private final BlockingQueue<Message> buffer;
    private final Plan plan;
    private ErrorHandler errorHandler;

    public void setCountMessages(int countMessages) {
        this.countMessages = countMessages;
    }

    private int countMessages;

    private final String brokerUrl;
    private final String queueName;
    private final int deliveryMode;
    private final int sessionMode;

    public MessageSender(String typeConnection,
                         String url,
                         String queueName,
                         int deliveryMode,
                         int sessionMode,
                         BlockingQueue<Message> buffer,
                         Plan plan) {
        this.buffer = buffer;
        this.queueName = queueName;
        this.deliveryMode = deliveryMode;
        this.sessionMode = sessionMode;
        this.plan = plan;
        this.brokerUrl = typeConnection + "://" + url;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setConnection() throws LoaderException {
        // Create a ConnectionFactory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);

        try {
            // Create a Connection
            connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            session = connection.createSession(false, sessionMode);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue(queueName);

            // Create a MessageProducer from the Session to the Topic or Queue
            producer = session.createProducer(destination);
            producer.setDeliveryMode(deliveryMode);
        } catch (JMSException e) {
            closeConnection();
            throw new LoaderException("Ошибка установки соединения с MQ.", e);
        }

    }

    public void closeConnection() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (JMSException e) {
            log.error("Ошибка закрытия соединения с MQ.", e);
            session = null;
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            log.error("Ошибка закрытия соединения с MQ.", e);
            connection = null;
        }
    }

    public void sendMessage(String textMessage) throws LoaderException {
        try {
            TextMessage message = session.createTextMessage(textMessage);
            producer.send(message);
        } catch (Exception e) {
            throw new LoaderException("Ошибка отправки сообщения: \n" + textMessage, e);
        }
    }

    @Override
    public void run() {
        try {
            int numberMessage = 1;
            log.info("Начало отправки: " + System.currentTimeMillis());
            while (numberMessage <= countMessages) {
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Поток завершился извне.");
                    break;
                }
                Message result;
                try {
                    result = buffer.take();
                } catch (InterruptedException e) {
                    log.error("Ошибка получения из очереди сообщения.\n "+ e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
                log.info("Отправлено №" + numberMessage + " в " + System.currentTimeMillis() + ": " + result);
                sendMessage(result.getData());
                numberMessage++;
            }
            log.info("Конец отправки: " + System.currentTimeMillis());
        } catch (LoaderException e) {
            errorHandler.closeGenerator(e);
        } finally {
            closeConnection();
        }
    }
}
