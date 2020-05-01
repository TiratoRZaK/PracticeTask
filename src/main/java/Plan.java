import java.util.ArrayList;
import java.util.List;

public class Plan {
    private final List<Stage> stages;
    public final int countMessage;

    public Plan(String plan) throws LoaderException {
        this.stages = new ArrayList<>();
        parsePlan(plan);
        countMessage = getCountMessagesInAllStages();
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
                throw new LoaderException("Ошибка чтения плана. Синтаксическая ошибка в скорости или времени отправки сообщений.", e);
            }
        }
    }

    public Stage getStage(int number) {
        if (number <= stages.size()) {
            return stages.get(number - 1);
        }
        return null;
    }

    private int getCountMessagesInAllStages() {
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
        private final int number;
        private final int speedSending;
        private final int timeLife;
        public final int countMessage;

        public Stage(int number, int speedSending, int timeLife) {
            this.number = number;
            this.speedSending = speedSending;
            this.timeLife = timeLife;
            countMessage = getCountMessages();
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

        private int getCountMessages() {
            return getSpeedSending() * getTimeLife();
        }
    }
}
