import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Reader {
    int current;
    byte[] input;



    public boolean EOF(){
        return current==input.length;
    }
    public Reader(String dataPath) throws IOException {
        input = Files.readAllBytes(Paths.get(dataPath));
        current = 0;
    }

    public int ReadInt(int bytes){
        int sum = 0;
        bytes--;
        int pow = 0;
        for(int i = bytes; i >= 0; i--){

            sum += (0xFF & input[current + i]) * Math.pow(16, pow);
            pow += 2;
        }
        current+=bytes + 1;
        return sum;
    }

    public String ReadDouble(int bytes, int accuracy){
        double sum = 0;
        bytes--;
        int pow = 0;
        for(int i = bytes; i >= 0; i--){

            sum += (0xFF & input[current + i]) * Math.pow(16, pow);
            pow += 2;
        }
        current+=bytes + 1;
        if(sum == 128 || sum == 32768)
            return String.valueOf((int)sum);
        else
        return String.valueOf(sum/Math.pow(10,accuracy));

    }

    public String ReadSymbols(int bytes){
        current+=bytes;
        return new String(Arrays.copyOfRange(input, current-bytes, current), Charset.forName("IBM500"));
    }

    public void Skip(int num){
        current+=num;
    }
}
