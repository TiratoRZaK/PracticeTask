import java.util.ArrayList;
import java.util.List;

public class Properties {
    private String typeConnection;
    private String URL;
    private String userName;
    private String password;

    private List<Stage> stages;

    public Properties(String typeConnection, String URL, String userName, String password) {
        this.typeConnection = typeConnection;
        this.userName = userName;
        this.password = password;
        this.URL = URL;
        this.stages = new ArrayList<>();
    }

    public void addStage(String number, String countMessages, String timeLife){
         stages.add(new Stage(number, countMessages, timeLife));
    }

    public Stage getStage(int number){
        if(number <= stages.size()){
            return stages.get(number-1);
        }
        return null;
    }

    public String getTypeConnection() {
        return typeConnection;
    }

    public String getURL() {
        return URL;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public Integer getCountMessagesInAllStages(){
        int summ = 0;
        for ( int i = 0 ; i< countStages(); i++) {
            summ += getStage(i+1).getCountMessages();
        }
        return summ;

    }

    public int countStages(){
        return stages.size();
    }

    public class Stage{
        private Integer number;
        private Integer speedSending;
        private Integer timeLife;

        public Stage(String number, String speedSending, String timeLife) {
            this.number = Integer.parseInt(number);
            this.speedSending = Integer.parseInt(speedSending);
            this.timeLife = Integer.parseInt(timeLife);
        }

        public Integer getSpeedSending() {
            return speedSending;
        }

        public Integer getTimeLife() {
            return timeLife;
        }

        public Integer getNumber(){
            return number;
        }

        public Integer getCountMessages(){
            return getSpeedSending()*getTimeLife();
        }
    }
}
