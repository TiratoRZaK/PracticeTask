import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Message implements Delayed {
    private final String data;
    private final long startTime;

    public Message(String data, long startTime) {
        this.data = data;
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getData() {
        return data;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (this.startTime - ((Message) o).startTime);
    }

    @Override
    public String toString() {
        return "Message {" +
                data +
                ", startTime=" + startTime +
                '}';
    }
}
