import java.util.ArrayList;
import java.util.List;

public class Plan {
    private List<Stage> stages;

    public Plan(String plan) throws LoaderException {
        this.stages = new ArrayList<>();
        parsePlan(plan);
    }

    public void addStage(int number, int countMessages, int timeLife) {
        stages.add(new Stage(number, countMessages, timeLife));
    }

    private void parsePlan(String plan) throws LoaderException {
        String[] stages = plan.split(";");
        for (int i = 0; i < stages.length; i++) {
            String[] prop = stages[i].split("/");
            String countMessages = prop[0];
            String timeLife = prop[1];
            try {
                addStage((i + 1), Integer.parseInt(countMessages), Integer.parseInt(timeLife));
            } catch (NumberFormatException e) {
                throw new LoaderException("Ошибка чтения плана. Файл Application.properties содержит ошибки.", e);
            }
        }
    }

    public Stage getStage(int number) {
        if (number <= stages.size()) {
            return stages.get(number - 1);
        }
        return null;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public int getCountMessagesInAllStages() {
        int summ = 0;
        for (int i = 0; i < countStages(); i++) {
            summ += getStage(i + 1).getCountMessages();
        }
        return summ;

    }

    public int countStages() {
        return stages.size();
    }

    public static class Stage {
        private int number;
        private int speedSending;
        private int timeLife;

        public Stage(int number, int speedSending, int timeLife) {
            this.number = number;
            this.speedSending = speedSending;
            this.timeLife = timeLife;
        }

        public int getSpeedSending() {
            return speedSending;
        }

        public int getTimeLife() {
            return timeLife;
        }

        public int getNumber() {
            return number;
        }

        public int getCountMessages() {
            return getSpeedSending() * getTimeLife();
        }
    }
}
