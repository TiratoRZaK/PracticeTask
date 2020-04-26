import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

public class Loader {
    private static String TYPE_CONNECTION;
    private static String USER_NAME;
    private static String PASSWORD;
    private static String URL;

    private static Plan PLAN;

    public static void main(String[] args) throws InterruptedException {
        parseProperties();

        BlockingQueue<Message> blockingQueue = new DelayQueue<Message>();
        MessageProvider provider = new MessageProvider(blockingQueue, PLAN);
        Thread t1 = new Thread(provider);
        t1.start();
        MessageSender sender = new MessageSender(
                TYPE_CONNECTION,URL,
                "TEST_QUEUE",
                DeliveryMode.NON_PERSISTENT,
                Session.AUTO_ACKNOWLEDGE,
                blockingQueue,
                PLAN
        );
        try {
            sender.setConnection();
        } catch (JMSException e) {
            System.out.println("Ошибка подключения к MQ. Причина: "+e);;
        }
        Thread t2 = new Thread(sender);
        t2.start();
    }

    private static String getPropertyValue(String propertyName){
        String propertyValue = "";

        java.util.Properties properties = new Properties();

        try(InputStream inputStream = new FileInputStream("./src/main/resources/Application.properties")){
            properties.load(inputStream);
            propertyValue = properties.getProperty(propertyName);
        }catch (IOException e){
            System.out.println(e);
        }
        return propertyValue;
    }

    private static void parseProperties() {
        TYPE_CONNECTION = getPropertyValue("typeConnection");
        URL = getPropertyValue("URL");
        USER_NAME = getPropertyValue("userName");
        PASSWORD = getPropertyValue("password");

        String plan = getPropertyValue("plan");
        PLAN = new Plan(plan);
    }
}