import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        try {
//            Analyzer analyzer = new Analyzer("tms.ddl","si11812d.mci1790019","result.txt");
            Analyzer analyzer = new Analyzer("tms.ddl","sib1934d.2","result2.txt");
            analyzer.Process();

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("done");
        }
    }

}
