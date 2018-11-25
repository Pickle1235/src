import java.io.*;
import java.util.ArrayList;
import java.util.regex.*;

/**
 * ChatFilter
 * <p>
 * Filter for chat
 *
 * @author Geon An, Arron Smith
 * @version November 26, 2018
 */

public class ChatFilter {
    private File file;
    private BufferedReader bfr;
    private ArrayList<String> badWords = new ArrayList<>();
    private String reading;

    public ChatFilter(String badWordsFileName) throws FileNotFoundException {
        this.file = new File(badWordsFileName);

        this.bfr = new BufferedReader(new FileReader(file));


        try {
            while ((reading = bfr.readLine()) != null) {
                badWords.add(reading);
            }
        } catch (IOException e) {
            System.out.println("IOException");
        }

    }

    public String filter(String msg) {

        for (String badWord : badWords) {
            if (msg.toLowerCase().contains(badWord)) {
                //String regex = "(\\b)" + badWord + "(\\b)";
                //Apparently they want trash censoring...

                String regex = badWord;

                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(msg.toLowerCase());

                String replace = "";
                for (int i = 0; i < badWord.length(); i++) {
                    replace += "*";
                }

                while (m.find()) {
                    msg = msg.substring(0, m.start()) + replace + msg.substring(m.end());
                }
            }
        }

        return msg;


    }
}
