import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Hello world!
 */
public class MQ {
    public static void thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }

    public static class HelloWorldProducer implements Runnable {
        private String message;
        private Session session;
        private MessageProducer producer;
        private Connection connection;

        public HelloWorldProducer(Properties properties, String queueName, int deliveryMode, int sessionMode) {
            try {
                String brokerURL = properties.getTypeConnection()+"://"+properties.getURL();
                setConnecting(brokerURL, queueName, deliveryMode, sessionMode);
            } catch (JMSException e) {
                System.out.println("Error connection. Caught: "+e);
                e.printStackTrace();
            }

        }

        public void setConnecting(String brokerUrl, String queueName, int deliveryMode, int sessionMode) throws JMSException {
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

        public void run() {
            try {
                sendMessage(message);
            }
            catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void closeConnection() throws JMSException {
            session.close();
            connection.close();
        }

        public void sendMessage(String textMessage) throws JMSException {
            // Create a messages
            String text = textMessage;
            TextMessage message = session.createTextMessage(text);

            // Tell the producer to send the message
            producer.send(message);
        }
    }

    public static class HelloWorldConsumer implements Runnable, ExceptionListener {
        public void run() {
            try {

                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

                // Create a Connection
                Connection connection = connectionFactory.createConnection();
                connection.start();

                connection.setExceptionListener(this);

                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("TESTIK :)");

                // Create a MessageConsumer from the Session to the Topic or Queue
                MessageConsumer consumer = session.createConsumer(destination);

                // Wait for a message
                Message message = consumer.receive(1000);

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();
                    System.out.println("Received " + System.currentTimeMillis() + " : " + text);
                } else {
                    //System.out.println("Received: " + message);
                }

                consumer.close();
                session.close();
                connection.close();
            } catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }
    }
}