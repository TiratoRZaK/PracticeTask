import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Message implements Delayed {
    private String data;
    private long startTime;

    public Message(String data, long delayInMilliseconds, long prevTime) {
        this.data = data;
        if(prevTime == 0L){
            this.startTime = System.currentTimeMillis() + delayInMilliseconds;
        }
        else {
            this.startTime = prevTime + delayInMilliseconds;
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public String getData(){
        return data;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return (int)(this.startTime - ((Message) o).startTime);
    }

    @Override
    public String toString() {
        return "Message {" +
                data+
                ", startTime=" + startTime +
                '}';
    }
}
