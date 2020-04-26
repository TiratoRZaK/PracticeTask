import java.util.ArrayList;
import java.util.List;

public class Plan {
    private List<Stage> stages;

    public Plan(String plan) {
        this.stages = new ArrayList<>();
        parsePlan(plan);
    }

    public void addStage(Integer number, String countMessages, String timeLife){
         stages.add(new Stage(number, countMessages, timeLife));
    }

    private void parsePlan(String plan){
        String[] stages = plan.split(";");
        for ( int i = 0; i < stages.length; i++ ) {
            String[] prop = stages[i].split("/");
            String countMessages = prop[0];
            String timeLife = prop[1];
            addStage((i+1), countMessages, timeLife);
        }
    }

    public Stage getStage(int number){
        if(number <= stages.size()){
            return stages.get(number-1);
        }
        return null;
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

        public Stage(Integer number, String speedSending, String timeLife) {
            this.number = number;
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
