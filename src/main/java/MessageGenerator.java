import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MessageGenerator
{
    List<String> selectedValue;
    StringBuilder message = new StringBuilder();

    public static void main(String[] args)
    {
        Map<String, Object> vars = new HashMap<>();
        vars.put("A", 1);
        vars.put("B", 5);
        List<String> selectedValue = new ArrayList<>();

    }
//  +  Генерация сообщения из файла шаблона. (Непонял откуда берём vars)
    public String generate(String pathTemplate) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(pathTemplate));
        while (sc.hasNextLine()) {
            /*
            CompiledTemplate template = TemplateCompiler.compileTemplate(sc.nextLine());

            message.append(TemplateRuntime.execute(template, new GeneratorMessage(), vars);
            */
        }
        return "";
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
    public String date(int addCountDay, String format){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDateTime now = LocalDateTime.now();
        //  +Добавление рабочих дней............................................................................
        return dtf.format(now).toString();
    }

    //  Выбор случайного значения из текстового файла.
    //  Значения в файле можно записать в одну колонку, каждое на новой строке.
    public String fileValue(String path) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(path));
        List<String> values = new ArrayList<>();
        while (sc.hasNextLine()){
            values.add(sc.nextLine());
        }
        sc.close();
        return (values.get(new Random().nextInt(values.size()))).trim();
    }
// +    Надо разобраться с vars
    //  Выбор любого случайного значения из файла кроме тех значений,
    //  которые уже были выбраны из этого файла для текущего сообщения.
    //  С помощью этой функции можно выбирать стороны сделки так, чтобы они были разными.
    public String uniqueFileValue(String path) {
        return "";
    }
}
