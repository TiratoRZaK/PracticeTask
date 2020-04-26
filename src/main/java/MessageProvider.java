import java.io.FileNotFoundException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

/*
* Он должен генерировать сообщения по шаблону и отправлять в DelayQueue по плану.
* DelayQueue ему можно передать в конструкторе. Так сейчас работает класс Producer.
* Только он отправляет сообщения через  ProducerConsumer. Но проще будет сразу в  DelayQueue.
*/
public class MessageProvider implements Runnable{
    private BlockingQueue<Message> buffer;
    private Plan plan;

    public MessageProvider(BlockingQueue<Message> buffer, Plan plan) {
        this.buffer = buffer;
        this.plan = plan;
    }

    @Override
    public void run() {
        try {
            System.out.println("Начало генерации: "+System.currentTimeMillis());
            MessageGenerator generator = new MessageGenerator();
            long prevTime = 0L;
            for (int i = 0; i < plan.countStages(); i++){
                Plan.Stage currentStage = plan.getStage(i+1);
                int generatedMessageCount = 0;
                System.out.println((i+1)+" этап начало: "+System.currentTimeMillis());
                while(generatedMessageCount < currentStage.getCountMessages()) {
                    long delay = (long) (currentStage.getTimeLife()*1000/currentStage.getCountMessages());
                    Message message = new Message(generator.generate(), delay, prevTime);
                    buffer.put(message);
                    System.out.println("Сформировано: "+message + "Этап: "+ currentStage.getNumber());
                    prevTime = message.getStartTime();
                    generatedMessageCount++;
                }
            }
            System.out.println("Конец генерации: "+System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
