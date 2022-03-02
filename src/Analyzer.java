import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Analyzer {
    //TODO: fill the dictionary for the GRPs (LNAT records on the bottom of description)
    List<String> desc;
    int currentPos;
    Reader reader;
    BufferedWriter writer;
    int rbodyType;
    int depth = 0;
    List<String> stack;
    String grName;
    int grLen;
    int grStart;
    Dictionary<String, Integer> counters;
    Dictionary<String, ArrayList<String>> definitions;

    public Analyzer(String descriptionFilePath, String DataFilePath, String Output) throws IOException {
        desc = Files.readAllLines(Paths.get(descriptionFilePath), Charset.forName("Windows-1251"));
        reader = new Reader(DataFilePath);
        writer = Files.newBufferedWriter(Paths.get(Output), Charset.forName("UTF-8"));
        currentPos = 0;
        rbodyType = 0;
        stack = new ArrayList<>();
        counters = new Hashtable<>();
        definitions = new Hashtable<>();
    }

    public void Process() throws IOException {
        GetDefinitions();
        while (!reader.EOF() && rbodyType != -1) {

            ParseHeader();
            ParseRecord(rbodyType);
        }

    }

    private void GetDefinitions() {
        for (int i = desc.size() - 1; i > 0; i--) {
            String[] temp = desc.get(i).split("\\s+;?|;");
            if (temp[0].contains("END"))
                break;
            if (temp[0].equals("LNAT")) {
                ArrayList<String> values = new ArrayList<>();
                for (String j : temp[2].split(",")) {
                    values.add(j);
                }
//                definitions.put(temp[1], values);
            }
        }
    }

    public int length() {
        return desc.size();
    }

    public void seek(int To) {
        currentPos = To;
    }

    public int currentLine() {
        return currentPos;
    }

    private boolean ParseLine(String grpName) throws IOException {
//        if(grLen + grStart <= reader.current){
//            depth = 0;
//            return false;
//        }

        String[] line = desc.get(currentPos).trim().split("\\s+;?|;");
        currentPos++;
        writer.flush();
        if (line[0].contains("GRP")) {
            writer.write("Новая группа: " + line[1]);
            writer.newLine();
            depth++;
            stack.add(line[1].replace("(", "").replace(")", "").trim());
            ParseGroup(0,line[1].replace("(", "").replace(")", "").trim());
        }

        if (line[0].contains("GRV") || line[0].contains("GRK")) {
            int len = 0;
            String name = line[1].replace("(", "").replace(")", "").trim();

            if(counters.get(name) == null) {
                name = line[0].split("\\(")[1].replace("(", "").replace(")", "").trim();
            }
            len = counters.get(name);
            stack.add(name);

            String searchname = "";
            for(int i = 0; i<line.length;i++){
                if(line[i].contains(")")){
                    searchname = line[i + 1];
                    break;
                }
            }
            writer.write("Новая группа GRV: " + searchname);
            writer.newLine();
            int grstart = currentPos;
            if(len != 0) {
                    currentPos = grstart;
                    depth++;
                    ParseGroup(len,searchname);

            }
            else{
                SeekToGRVEnd(searchname);
            }
        }

        if (line[0].equals("MIT")) {
            writer.write(line[1] + ":");
            if (line[2].contains("A")) {
                int len = Integer.valueOf(line[2].substring(line[2].indexOf("(") + 1, line[2].indexOf(")")));
                String data = reader.ReadSymbols(len);
                writer.write(data);
            }
            if (line[2].contains("B")) {
                int len = Integer.valueOf(line[2].substring(line[2].indexOf("(") + 1, line[2].indexOf(")")));
                Object data = null;
                int acc = -1;
                for (String i : line) {
                    if (i.contains("D")) {
                        acc = Integer.valueOf(i.substring(i.indexOf("(") + 1, i.indexOf(")")));
                    }
                }
                if (acc == -1)
                    data = reader.ReadInt(len);
                else
                    data = reader.ReadDouble(len, acc);
                writer.write(String.valueOf(data));
            }
            PrintComment(line);
            writer.newLine();
            writer.flush();
        }

        if (line[0].contains("CHA")) {
            int len = 0;
            writer.write(line[0].replace("(", " ").replace(")", "").trim() + " ");
            for (int i = 1; i < line.length; i++) {
                if (line[i].length() > 0) {
                    if (line[i].charAt(0) == 'B') {
                        len = Integer.valueOf(line[i].substring(line[i].indexOf("(") + 1, line[i].indexOf(")")));
                    }
                    if (line[i].charAt(0) == 'Q') {
                        writer.write(line[i] + ": ");
                    }
                }
            }
            writer.write(String.valueOf(reader.ReadInt(len)));
            PrintComment(line);
            writer.newLine();
            writer.flush();
        }

        if (line[0].contains("KEY")) {
            int len = 0;
            writer.write("Тут был ключ");
            writer.write(line[0].replace("(", " ").replace(")", "").trim() + ":");
            for (int i = 1; i < line.length; i++) {
                if (line[i].length() > 0) {
                    if (line[i].charAt(0) == 'B') {
                        len = Integer.valueOf(line[i].substring(line[i].indexOf("(") + 1, line[i].indexOf(")")));
                    }
                }
            }

            writer.write(String.valueOf(reader.ReadInt(len)) + line[1]);
            PrintComment(line);
            writer.newLine();
            writer.flush();
        }

        if (line[0].contains("CNT")) {
            int len = 0;
            for (int i = 1; i < line.length; i++) {
                if (line[i].length() > 0) {
                    if (line[i].charAt(0) == 'B') {
                        len = Integer.valueOf(line[i].substring(line[i].indexOf("(") + 1, line[i].indexOf(")")));
                    }
                }
            }
            int temp = reader.ReadInt(len);
            counters.put(line[1], temp);
            writer.write(line[0] + " " + line[1] + ": " + temp);
            PrintComment(line);
            writer.newLine();
            writer.flush();
        }

        if (line[0].contains("END")) {
            if (line[1].equals(grName))
                depth--;
            else{
                if(grpName != null){
                    return !grpName.equals(line[1]);
                }
            }
        }


        return true;
    }

    private void SeekToGRVEnd(String name) throws IOException {
        for (int i = currentPos + 1; i < desc.size(); i++) {
            String[] temp = desc.get(i).trim().split("\\s+;?|;");
            if(temp[0].equals("END")){
                if(temp[1].equals(name)) {
                    currentPos = i + 1;
                    break;
                }
            }
        }
        writer.write("Группа имела длину 0, поэтому пропушена\n");

    }

    private void PrintComment(String[] line) throws IOException {
        boolean flag = false;
        for (int i = 0; i < line.length; i++) {
            if (line[i].contains("//")) {
                flag = true;
            }
            if (flag) {
                writer.write(" " + line[i]);
            }
        }
    }

    private void ParseGroup(int length, String grpName) throws IOException {

        String[] line = desc.get(currentPos).trim().split("\\s+;?|;");
        int len = length;
        if(len == 0)
        len = Integer.valueOf(line[0].substring(line[0].indexOf("(") + 1, line[0].indexOf(")")));
        String name = "";
        for(int i = 0; i< line.length; i++){
            if(line[i].length() > 0)
            if(line[i].charAt(0) == 'C'){
                name = line[i].replace("C","").replace("(","").replace(")","");
            }
        }
        //currentPos++;
        int start = currentPos;
        int count = 0;


        for (int i = 0; i < len; i++) {
            if(definitions.get(name) != null){
                writer.write("INDEX: " + definitions.get(name).get(i));
            }
            count = 0;
            while (ParseLine(grpName)) {
                count++;
            }
            if(i + 1 < len)
                currentPos = start;
        }
        //currentPos += count;
        depth--;
        writer.write("Конец группы: " + grpName);
        writer.newLine();
        writer.newLine();
        writer.flush();
    }

    private void ParseRecord(int type) throws IOException {
        SeekToDefinition(type);
        while (counters.keys().hasMoreElements()) {
            counters.remove(counters.keys().nextElement());
        }
        grStart = reader.current;
        while (depth > 0 ) {
            ParseLine(null);
        }
        depth = 0;

    }

    private void SeekToDefinition(int type) throws IOException {
        for (int j = 0; j < desc.size(); j++) {
            String i = desc.get(j);
            int temp = i.indexOf("RBODY(" + type);
            if (temp != -1) {
                grName = i.trim().split("\\s+;?|;")[1];
                currentPos = j + 1;
                depth += 1;
                break;
            }
            else{

            }
        }
    }

    public void ParseHeader() throws IOException {
        grLen = reader.ReadInt(2);
        reader.Skip(2);

        writer.write("\n\nДЛЗАП: " + grLen + " НАЧАЛО ЗАГОЛОВКА");
        writer.newLine();
        writer.write("ГОД: " + reader.ReadInt(2));
        writer.newLine();
        writer.write("МЕСЯЦ: " + reader.ReadInt(1));
        writer.newLine();
        writer.write("СТАНЦИЯ: " + reader.ReadInt(4));
        writer.newLine();
        rbodyType = reader.ReadInt(1);
        writer.write("ТИПЗАП: " + rbodyType);
        writer.newLine();
        writer.newLine();
        grLen -=12;
        writer.flush();
        if(rbodyType == 0) {
            rbodyType = -1;
        }
    }


}