import org.apache.commons.io.FileUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MessageGenerator {
    private Map<String, List<String>> usedValues;
    private String template;
    private final Map<String, List<String>> files;

    public MessageGenerator(Map<String, List<String>> files) throws LoaderException {
        loadTemplate();
        this.files = files;
    }

    private void loadTemplate() throws LoaderException {
        try {
            template = FileUtils.readFileToString(new File("configs/template.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LoaderException("Ошибка чтения файла шаблона сообщений.", e);
        }
    }

    public String generate() {
        usedValues = new HashMap<>();
        Map<String, Object> vars = new HashMap<>();
        CompiledTemplate templ = TemplateCompiler.compileTemplate(template);
        return TemplateRuntime.execute(templ, this, vars).toString();
    }

    //  Генерация случайного целого числа в указанном диапазоне (границы входят в диапазон)
    public int integer(int minValue, int maxValue) {
        return (int) ((Math.random() * (maxValue - minValue + 1) + minValue));
    }

    //  Генерация случайного десятичного числа в указанном диапазоне(границы входят в диапазон)
    //  с указанным количеством знаков после точки.
    public double decimal(double minValue, double maxValue, int precision) {
        Double randDouble = (Double) (Math.random() * (maxValue - minValue + Double.MIN_VALUE) + minValue);
        BigDecimal bd = new BigDecimal(Double.toString(randDouble));
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    //  Гникальный идентификатор в формате UUID.
    public UUID uuid() {
        return UUID.randomUUID();
    }

    //  Генерация строки, содержащей дату и время в указанном формате с заданным сдвигом в рабочих днях.
    //  Например, если сегодня пятница и сдвиг равен 2м дням, мы должны получить вторник.
    public String dateTime(int addCountDay, String format) {
        LocalDateTime resultDateTime = LocalDateTime.now();
        int daysToAdd = addCountDay;
        while (daysToAdd != 0) {
            resultDateTime = resultDateTime.plusDays(1);
            if (resultDateTime.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    resultDateTime.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysToAdd--;
            }
        }
        return resultDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    //  Выбор случайного значения из текстового файла.
    //  Значения в файле можно записать в одну колонку, каждое на новой строке.
    public String fileValue(String fileName) {
        List<String> values = files.get(fileName);
        int randomValue = new Random().nextInt(values.size());

        if (usedValues.containsKey(fileName)) {
            usedValues.get(fileName).add(values.get(randomValue));
        } else {
            List<String> list = new ArrayList<>();
            list.add(values.get(randomValue));
            usedValues.put(fileName, list);
        }
        return values.get(randomValue);
    }

    //  Выбор любого случайного значения из файла кроме тех значений,
    //  которые уже были выбраны из этого файла для текущего сообщения.
    //  С помощью этой функции можно выбирать стороны сделки так, чтобы они были разными.
    public String uniqueFileValue(String fileName) throws Exception {
        List<String> values = files.get(fileName);
        String selectedRandomValue;
        if (usedValues.containsKey(fileName)) {
            List<String> usedValues = new ArrayList<>(this.usedValues.get(fileName));
            List<String> uniqueValues = values.stream().filter(e -> !usedValues.contains(e)).collect(Collectors.toList());

            if (uniqueValues.size() == 0) {
                throw new LoaderException("Ошибка формирования сообщения. Причина: " +
                        "Запрашивается больше уникальных значений, чем есть в файле!");
            }

            int randomValue = new Random().nextInt(uniqueValues.size());
            selectedRandomValue = uniqueValues.get(randomValue);
            this.usedValues.get(fileName).add(selectedRandomValue);
        } else {
            List<String> list = new ArrayList<>();
            int randomValue = new Random().nextInt(values.size());
            selectedRandomValue = values.get(randomValue);
            list.add(selectedRandomValue);
            usedValues.put(fileName, list);
        }
        return selectedRandomValue;
    }
}
