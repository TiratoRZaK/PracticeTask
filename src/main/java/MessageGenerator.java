import org.apache.commons.io.FileUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    private Map<String, List<String>> usedFiles;
    private String template;

    public MessageGenerator() throws LoaderException {
        loadTemplate("./src/main/resources/template.txt");
        usedFiles = new HashMap<>();
    }

    private void loadTemplate(String pathTemplate) throws LoaderException {
        try {
            template = FileUtils.readFileToString(new File(pathTemplate), StandardCharsets.UTF_8);
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
        Integer daysToAdd = addCountDay;
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
    public String fileValue(String path) throws LoaderException {
        List<String> values = new ArrayList<>();
        if (!usedFiles.containsKey(path)) {
            try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
                String line;
                while ((line = br.readLine()) != null) {
                    values.add(line);
                }
            } catch (IOException e) {
                throw new LoaderException("Ошибка чтения файла.", e);
            }
            usedFiles.put(path, values);
        } else {
            values = usedFiles.get(path);
        }
        Integer randomValue = new Random().nextInt(values.size());

        if (usedValues.containsKey(path)) {
            usedValues.get(path).add(values.get(randomValue));
        } else {
            List<String> list = new ArrayList<>();
            list.add(values.get(randomValue));
            usedValues.put(path, list);
        }
        return values.get(randomValue);
    }

    //  Выбор любого случайного значения из файла кроме тех значений,
    //  которые уже были выбраны из этого файла для текущего сообщения.
    //  С помощью этой функции можно выбирать стороны сделки так, чтобы они были разными.
    public String uniqueFileValue(String path) throws Exception {
        List<String> values = new ArrayList<>();
        if (!usedFiles.containsKey(path)) {
            try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
                String line;
                while ((line = br.readLine()) != null) {
                    values.add(line);
                }
            } catch (IOException e) {
                throw new LoaderException("Ошибка чтения файла.", e);
            }
            usedFiles.put(path, values);
        } else {
            values = usedFiles.get(path);
        }

        String selectedRandomValue;
        if (usedValues.containsKey(path)) {
            List<String> usedValues = new ArrayList<>(this.usedValues.get(path));

            List<String> uniqueValues = values.stream().filter(e -> !usedValues.contains(e)).collect(Collectors.toList());

            if (uniqueValues.size() == 0) {
                throw new LoaderException("Ошибка формирования сообщения. Причина: " +
                        "Запрашивается больше уникальных значений, чем есть в файле!");
            }

            int randomValue = new Random().nextInt(uniqueValues.size());
            selectedRandomValue = uniqueValues.get(randomValue);
            this.usedValues.get(path).add(selectedRandomValue);
        } else {
            List<String> list = new ArrayList<>();
            int randomValue = new Random().nextInt(values.size());
            selectedRandomValue = values.get(randomValue);
            list.add(selectedRandomValue);
            usedValues.put(path, list);
        }
        return selectedRandomValue;
    }
}
