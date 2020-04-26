import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.concurrent.BlockingQueue;

/*
 * Его задачи:
 *   подключиться к MQ,
 *   читать сообщения из DelayQueue,
 *   отправлять в MQ,
 *   закрыть соединение, когда все сообщения будут отправлены.
 * */
public class MessageSender implements Runnable{
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private BlockingQueue<Message> buffer;
    private Plan plan;

    private String brokerUrl;
    private String queueName;
    private int deliveryMode;
    private int sessionMode;

    public MessageSender(String typeConnection, String url, String queueName, int deliveryMode, int sessionMode, BlockingQueue<Message> buffer, Plan plan) {
        this.buffer = buffer;
        this.queueName = queueName;
        this.deliveryMode = deliveryMode;
        this.sessionMode = sessionMode;
        this.plan = plan;
        this.brokerUrl = typeConnection+"://"+ url;
    }

    public void setConnection() throws JMSException {
        // Create a ConnectionFactory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);

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
    }

    public void closeConnection() throws JMSException {
        session.close();
        connection.close();
    }

    public void sendMessage(String textMessage) throws JMSException {
        TextMessage message = session.createTextMessage(textMessage);
        producer.send(message);
    }

    @Override
    public void run() {
        int numberMessage = 1;
        System.out.println("Начало отправки: "+System.currentTimeMillis());
        while (numberMessage <= plan.getCountMessagesInAllStages()){
            Message result = null;
            try {
                result = buffer.take();
            } catch (InterruptedException e) {
                System.out.println("Ошибка получения из очереди сообщения №"+numberMessage+". Причина: "+e);
            }
            System.out.println("Отправлено №" + numberMessage + " в " + System.currentTimeMillis() + ": " + result);
            try {
                sendMessage(result.getData());
            } catch (JMSException e) {
                System.out.println("Ошибка отправки сообщения №"+numberMessage+". Причина: "+e);
            }
            numberMessage++;
        }
        System.out.println("Конец отправки: "+System.currentTimeMillis());
        try {
            Thread.sleep(2000);
            closeConnection();
        } catch (InterruptedException | JMSException e) {
            System.out.println("Ошибка закрытия соединений с MQ. Причина: "+e);
        }

    }
}
