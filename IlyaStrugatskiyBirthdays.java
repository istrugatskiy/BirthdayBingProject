// I did this partially on my desktop so the package may be incorrect
package Ch3Parameters;
// Todo: make work with some birthdays. Done
// Todo: implement bing API. Done
// Todo: make dates consistent across app. Done
// Todo: put API key in a separate file. Done
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class IlyaStrugatskiyBirthdays {
    // Defines azure config
    // No key for now, unless you need it because you know
    private static final String host = "https://api.bing.microsoft.com";
    private static final String path = "/v7.0/custom/search";
    private static String subscriptionKey = "haha no screw off";
    private static final String customConfigId = "f4689edd-c038-42ff-a524-a71ec059ca9c";
    private static final String searchTerm = "Birthday facts for";

    public static void main(String[] args) {
        // Loads the API key from the file because I want to keep it secret.
        try {
            initApiKey();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write method is identical to System.out.print(), it's just faster to write
        write("This program compares two birthdays\n" +
                "and displays which one is sooner.\n");
        write("Today is " + getCurrentFormattedDate() + ", day #" + getCurrentDate().getDayOfYear() + " of the year.\n\n");
        renderedPersonData person1 = renderPerson(1);
        renderedPersonData person2 = renderPerson(2);
        if(person1.nextBirthdayPercent > person2.nextBirthdayPercent) {
            write("Person 2's birthday is sooner.\n\n");
        }
        else if(person2.nextBirthdayPercent > person1.nextBirthdayPercent) {
            write("Person 1's birthday is sooner.\n\n");
        }
        else {
            write("Wow, you share the same birthday!\n\n");
        }
        write("Person 1 birthday fact:\n");
        getBirthdayFact(person1.birthdayDate);
        // I'm on free so rate limiting and stuff
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        write("\nPerson 2 birthday fact:\n");
        getBirthdayFact(person2.birthdayDate);
    }

    private static renderedPersonData renderPerson(int personId) {
        write("Person " + personId + ":\n");
        write("What month and day were you born? ");
        Scanner scanner = new Scanner(System.in);
        String data = scanner.nextLine();
        String[] tempDate = data.split("\\s+");
        // Handles single digit months and days
        if(tempDate[0].length() == 1) {
            data = "0" + data;
        }
        if(tempDate[1].length() == 1) {
            data = new StringBuilder(data).insert(data.length() - 1, "0").toString();
        }
        // Formats string to correct format
        data = data.replace(' ', '-');
        data += "-" + getCurrentDate().getYear();
        LocalDate birthdayDate = null;
        // Have to catch exceptions cause "good practices" and stuff
        try {
            birthdayDate = formatDate(data);
        } catch (Exception e) {
            write("Inputting invalid dates I see?\n" +
                    "Do you have anything better to do?");
            System.exit(420);
        }
        int nextBirthday = daysBetween(birthdayDate);
        double nextBirthdayPercent = ((double) Math.round(((double) nextBirthday / (double) getDaysInYear()) * 1000) / 10);
        write(data + " falls on day #" + birthdayDate.getDayOfYear() + " of " + getDaysInYear() + ".\n");
        write(nextBirthdayPercent != 0 ? "Your next birthday is in " + nextBirthday + " day(s).\n": "");
        write(nextBirthdayPercent != 0 ? "That is " + nextBirthdayPercent + " percent of a year away.\n\n": "Happy birthday!\n\n");
        renderedPersonData returnData = new renderedPersonData();
        returnData.birthdayDate = birthdayDate;
        returnData.nextBirthdayPercent = nextBirthdayPercent;
        return returnData;
    }

    // Returns days in this year
    private static int getDaysInYear() {
        int year = getCurrentDate().getYear();
        if(year % 4 == 0 && year % 100 != 0) {
            return 366;
        }
        else if(year % 400 == 0) {
            return 366;
        }
        else {
            return 365;
        }
    }

    private static void getBirthdayFact(LocalDate birthday) {
        write("Attempting to contact Bing...\n\n");
        String data = "";
        if(!subscriptionKey.equals("")) {
            try {
                SearchResults result = SearchWeb(searchTerm + " " + (birthday.getMonth() + "-" + birthday.getDayOfMonth()));
                data = parseData(result.jsonResponse);
            }
            catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(1);
            }
        }
        else {
            write("ERR: API key not supplied\n");
            System.exit(420);
        }
        write(new StringBuilder(data).deleteCharAt(0).deleteCharAt(data.length()-2).toString().replace(". ", ".\n") + "\n");
    }

    // Formats date properly
    private static LocalDate formatDate(String data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        formatter = formatter.withLocale(Locale.ENGLISH );
        return LocalDate.parse(data, formatter);
    }

    // Returns days between two dates
    private static int daysBetween(LocalDate d1){
        if(d1.getDayOfYear() - getCurrentDate().getDayOfYear() >= 0) {
            return d1.getDayOfYear() - getCurrentDate().getDayOfYear();
        }
        else {
            int offset = d1.getDayOfYear() - getCurrentDate().getDayOfYear();
            return (offset - 1) + getDaysInYear();
        }
    }

    // Returns a properly formatted current date
    private static String getCurrentFormattedDate() {
        // Flips dates around to be more consistent
        return (getCurrentDate().getMonthValue() + "-" + getCurrentDate().getDayOfMonth() + "-" + getCurrentDate().getYear());
    }

    // Returns the current date
    private static LocalDate getCurrentDate() {
        return java.time.LocalDate.now();
    }

    // Prints a string of data
    private static void write(String data) {
        System.out.print(data);
    }

    // For returning multiple values from the renderPerson method
    private static class renderedPersonData {
        double nextBirthdayPercent;
        LocalDate birthdayDate;
    }

    // Returns a search result
    private static SearchResults SearchWeb (String searchQuery) throws Exception {
        // construct the URL for your search request (endpoint + query string)
        URL url = new URL(host + path + "?q=" +  URLEncoder.encode(searchQuery, StandardCharsets.UTF_8) + "&CustomConfig=" + customConfigId);
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        // receive the JSON body
        InputStream stream = connection.getInputStream();
        String response = new Scanner(stream).useDelimiter("\\A").next();

        // construct result object for return
        SearchResults results = new SearchResults(new HashMap<>(), response);

        stream.close();
        return results;
    }

    // Data return format
    private static class SearchResults {
        HashMap<String, String> relevantHeaders;
        String jsonResponse;
        SearchResults(HashMap<String, String> headers, String json) {
            relevantHeaders = headers;
            jsonResponse = json;
        }
    }

    // Loads API key from external file
    private static void initApiKey() throws IOException {
        // The slashes might be different on windows cause I am using linux
        subscriptionKey = Files.readString(Path.of(System.getProperty("user.dir") + "/out/production/CompSciJava/Ch3Parameters/key"), StandardCharsets.US_ASCII);
    }

    // Parses data from the received JSON object and gets the important part
    private static String parseData(String json_text) {
        JsonArray json = JsonParser.parseString(json_text).getAsJsonObject().get("webPages").getAsJsonObject().get("value").getAsJsonArray();
        int randomData = Math.random() >= 0.5 ? 0 : 1;
        JsonObject webPageFirstChild = json.get(randomData).getAsJsonObject();
        return webPageFirstChild.get("snippet").toString();
    }
}





