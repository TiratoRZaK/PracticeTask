import org.apache.commons.io.FileUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MessageGenerator
{
    private Map<String, List<Integer>> usedValue;
    private String template;

    public MessageGenerator() {
        loadTemplate("./src/main/resources/template.txt");
    }

    private void loadTemplate(String pathTemplate){
        try {
            template = FileUtils.readFileToString(new File(pathTemplate), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла шаблона сообщений. Причина: "+e);
        }
    }

    public String generate() {
        usedValue = new HashMap<>();
        Map<String, Object> vars = new HashMap<>();
        CompiledTemplate templ = TemplateCompiler.compileTemplate(template);
        return TemplateRuntime.execute(templ, this, vars).toString();
    }

    //  Генерация случайного целого числа в указанном диапазоне (границы входят в диапазон)
    public int integer(int minValue, int maxValue){
        return (int)(( Math.random() * (maxValue - minValue + 1) + minValue));
    }

    //  Генерация случайного десятичного числа в указанном диапазоне(границы входят в диапазон)
    //  с указанным количеством знаков после точки.
    public double decimal(double minValue, double maxValue, int precision){
        Double randDouble = (Double)(Math.random() * (maxValue - minValue + Double.MIN_VALUE) + minValue);
        BigDecimal bd = new BigDecimal(Double.toString(randDouble));
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    //  Гникальный идентификатор в формате UUID.
    public UUID uuid(){
        return UUID.randomUUID();
    }

    //  Генерация строки, содержащей дату и время в указанном формате с заданным сдвигом в рабочих днях.
    //  Например, если сегодня пятница и сдвиг равен 2м дням, мы должны получить вторник.
    public String dateTime(int addCountDay, String format){
        LocalDateTime resultDateTime = LocalDateTime.now();
        Integer daysToAdd = addCountDay;
        while(daysToAdd != 0){
            resultDateTime = resultDateTime.plusDays(1);
            if(     resultDateTime.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    resultDateTime.getDayOfWeek() != DayOfWeek.SUNDAY){
                daysToAdd--;
            }
        }
        return resultDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    //  Выбор случайного значения из текстового файла.
    //  Значения в файле можно записать в одну колонку, каждое на новой строке.
    public String fileValue(String path) {
        List<String> values = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(path))){
            while (sc.hasNextLine()) {
                values.add(sc.nextLine());
            }
        } catch (FileNotFoundException e){
            System.out.println("Файл не найден");
        }
        Integer randomValue = new Random().nextInt(values.size());

        if(usedValue.containsKey(path)){
            usedValue.get(path).add(randomValue);
        }else {
            List<Integer> list = new ArrayList<>();
            list.add(randomValue);
            usedValue.put(path, list);
        }

        return values.get(randomValue).trim();
    }

    //  Выбор любого случайного значения из файла кроме тех значений,
    //  которые уже были выбраны из этого файла для текущего сообщения.
    //  С помощью этой функции можно выбирать стороны сделки так, чтобы они были разными.
    public String uniqueFileValue(String path) {
        List<String> values = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(path))){
            while (sc.hasNextLine()) {
                values.add(sc.nextLine());
            }
        } catch (FileNotFoundException e){
            System.out.println("Файл не найден");
        }

        Integer randomValue;
        if(usedValue.containsKey(path)){
            List<Integer> usedValues = new ArrayList<>(usedValue.get(path));
            Integer i = new Random().nextInt(values.size());
            while (usedValues.contains(i)){
                i = new Random().nextInt(values.size());
            }
            randomValue = i;
            usedValue.get(path).add(randomValue);
        }else {
            List<Integer> list = new ArrayList<>();
            randomValue = new Random().nextInt(values.size());
            list.add(randomValue);
            usedValue.put(path, list);
        }
        return values.get(randomValue).trim();
    }
}
