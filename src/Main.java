import com.sun.xml.internal.ws.commons.xmlutil.Converter;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {




    public static void main(String[] args) {
        try {
            Analyzer analyzer = new Analyzer("tms.ddl","si11812d.mci1790019","kek.txt");
            analyzer.Process();
//            byte[] a = Files.readAllBytes(Paths.get("si11812d.mci1790019"));
//
//            System.out.println(ConvertToInt(a,8,11));
//            List<String> desc = Files.readAllLines(Paths.get("tms.ddl"), Charset.forName("Windows-1251"));
//            BufferedWriter bw = Files.newBufferedWriter(Paths.get("result.txt"),Charset.forName("UTF-8"));
//            for (int i = 0; i <12 ; i++) {
//                System.out.println(Integer.toHexString(0xFF & a[i]) + " " +(0xFF & a[i]));
//            }
//            int offset = 0;
//                while (offset + 12 < a.length) {
//                    int[] conf = ParseHeader(a, bw, offset);
//                    offset += 12;
//                    ParseRecord(a, bw, desc, conf, offset);
//                    System.out.println(offset);
//                    offset += conf[0] -12;
//                }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {

        }
    }

    private static void ParseRecord(byte[] input, BufferedWriter bw, List<String> desc, int[] conf, int offset) throws IOException {
        int type = conf[1];
        int descOffset = GetDescriptionOffset(desc,type);
        String recname = desc.get(descOffset).trim().split("\\s+")[1];
        recname = recname.replace(";","");
        bw.write("RECORD TYPE: " + recname + " : " + type + " ТУТ НОВОЕ ТЕЛО ЗАПИСИ");
        bw.newLine();
        String wrap;
        int descEnd = GetDescriptionEnd(desc, recname);
        int depth = 1;
        ArrayList<String> GRPList = new ArrayList<>();

        int i = descOffset+1;
        while (i < descEnd) {
            String[] info = desc.get(i).trim().split("\\s+");
            if(info[0].equals("MIT")){
                bw.write(info[1] + ":");
                if(offset < 500)
                System.out.println(info[1] + "  offset:" + offset);
                if(info[2].contains("A")){
                    int len = Integer.valueOf(info[2].substring(info[2].indexOf("(") + 1, info[2].indexOf(")")));
                    String data = ParseSymbols(input, bw, offset, len);
                    bw.write(data);
                    bw.newLine();
                    bw.flush();
                    offset+=len;
                }
                if(info[2].contains("B")){
                    int len = Integer.valueOf(info[2].substring(info[2].indexOf("(") + 1, info[2].indexOf(")")));
                    int data = ConvertToInt(input, offset, offset + len - 1);
                    bw.write(String.valueOf(data));
                    bw.newLine();
                    bw.flush();
                    offset+=len;
                }
            }

            if(info[0].equals("GRP")){

                GRPList.add(info[1]);
                depth+=1;
            }
            if(info[0].contains("IND")){
                int len = Integer.valueOf(info[0].substring(info[0].indexOf("(") + 1, info[0].indexOf(")")));
                offset+=len-1;

            }
            i++;  //while
        }

    }
    private void ReadLine(byte[] input, BufferedWriter bw, List<String> desc, int offset, StringBuilder wrap){

    }
    private int ParseGroup(byte[] input, BufferedWriter bw, List<String> desc, int offset) throws IOException {
        int len = 0;
        return offset + len;
    }

    private static int GetDescriptionEnd(List<String> desc, String recname) {
        int count = 0;
        for (String i : desc) {
            int temp = i.indexOf("END " + recname);
            if (temp != -1){
                return count;
            }
            count++;
        }
        return -1;
    }


    private static int GetDescriptionOffset(List<String> desc, int type) {
        int count = 0;
        for (String i : desc) {
            int temp = i.indexOf("RBODY(" + type);
            if (temp != -1){
                return count;
            }
            count++;
        }
        return -1;
    }

    private static int[] ParseHeader(byte[] input, BufferedWriter writer, int start) throws IOException {
        int len = ConvertToInt(input, start,start+1);
        int type = ConvertToInt(input, start+11,start+11);
        writer.write("ДЛЗАП: " + len + " НАЧАЛО ЗАГОЛОВКА");
        writer.newLine();
        writer.write("ГОД: " + ConvertToInt(input, start+4,start+5));
        writer.newLine();
        writer.write("МЕСЯЦ: " + ConvertToInt(input, start+6,start+6));
        writer.newLine();
        writer.write("СТАНЦИЯ: " + ConvertToInt(input, start+7,start+10));
        writer.newLine();
        writer.write("ТИПЗАП: " + type);
        writer.newLine();
        writer.flush();
        return new int[] {len,type};
    }

    private static int ConvertToInt(byte[] input, int start, int end){
        int sum = 0;
        int pow = 0;
        for(int i = end-start; i >= 0; i--){

            sum += (0xFF & input[start + i]) * Math.pow(16, pow);
            pow += 2;
        }
        return sum;
    }

    private static String ParseSymbols(byte[] input, BufferedWriter writer, int start,int len) throws IOException {
        return "a";
    }
}
