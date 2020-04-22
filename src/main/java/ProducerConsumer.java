import java.util.concurrent.DelayQueue;

public class ProducerConsumer {
    private DelayQueue<Message> buffer;
    private Properties properties;

    public ProducerConsumer(Properties properties) {
        this.properties = properties;
        this.buffer = new DelayQueue<>();
    }

    public Properties getProperties() {
        return properties;
    }

    synchronized void produce(Message message, Properties.Stage stage) throws InterruptedException {
        if(buffer.size() == stage.getCountMessages()){
            Thread.currentThread().stop();
        }
        buffer.put(message);
        System.out.println("Сформировано: "+message + "Этап: "+ stage.getNumber());
        notify();
    }

    synchronized int consume(int messageNumber) throws InterruptedException {
        while(buffer.size() == 0){
            wait();
        }
        Message result = buffer.take();
        System.out.println("Отправлено №" + messageNumber + " в " + System.currentTimeMillis() + ": " + result);

        notify();
        return ++messageNumber;
    }
}
