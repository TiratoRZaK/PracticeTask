
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Loader {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        Properties properties = parseProperties();

        ProducerConsumer sender = new ProducerConsumer(properties);
        Thread t1 = new Thread(new Producer(sender));
        t1.start();
        Thread t2 = new Thread(new Consumer(sender));
        t2.start();
    }

    private static Properties parseProperties() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse("./src/main/resources/Properties.xml");

        Properties prop;

        Node properties = document.getDocumentElement();
        NodeList settingsChildNodes = properties.getChildNodes();

        Node settingConnection = settingsChildNodes.item(3);
        NodeList settings = settingConnection.getChildNodes();
        prop = new Properties(
                settings.item(1).getTextContent(),
                settings.item(3).getTextContent(),
                settings.item(5).getTextContent(),
                settings.item(7).getTextContent()
        );

        Node plan = settingsChildNodes.item(1);
        NodeList stages = plan.getChildNodes();

        for (int j = 1; j < stages.getLength(); j=j+2) {
            Node stage = stages.item(j);
            NodeList attributesStage = stage.getChildNodes();
            Node countMessages = attributesStage.item(1);
            Node timeLife = attributesStage.item(3);

            prop.addStage(
                    stage.getAttributes().getNamedItem("number").getTextContent(),
                    countMessages.getTextContent(),
                    timeLife.getTextContent()
            );
        }
        return prop;
    }
}

class Producer implements Runnable {
    public ProducerConsumer sender;

    public Producer(ProducerConsumer sender) {
        this.sender = sender;
    }

    public void run() {
        try {
            for (int i = 0; i < sender.getProperties().countStages(); i++){
                Properties.Stage currentStage = sender.getProperties().getStage(i+1);
                int generatedMessageCount = 0;
                long prevTime = 0L;
                while(generatedMessageCount < currentStage.getCountMessages()) {
                    long delay = (long) (currentStage.getTimeLife()*1000/currentStage.getCountMessages());
                    Message message = new Message(new GeneratorMessage().fileValue("./src/main/resources/Properties.xml"), delay, prevTime);
                    sender.produce(message, currentStage);
                    prevTime = message.getStartTime();
                    generatedMessageCount++;
                }
            }
        } catch (FileNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Consumer implements Runnable {
    public ProducerConsumer sender;

    public Consumer(ProducerConsumer sender) {
        this.sender = sender;
    }

    public void run() {
        try {
            MQ.HelloWorldProducer producer = new MQ.HelloWorldProducer(sender.getProperties(), "TEST_QUEUE", DeliveryMode.NON_PERSISTENT, Session.AUTO_ACKNOWLEDGE);
            int countMessage = 1;
            while (countMessage<= sender.getProperties().getCountMessagesInAllStages()){
                countMessage = sender.consume(countMessage, producer);
            }
            Thread.sleep(5000);
            producer.closeConnection();
        } catch (InterruptedException | JMSException e) {
            e.printStackTrace();
        }
    }
}
