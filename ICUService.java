package eu.izadpanah.mcheck;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.StringJoiner;

public class ICUService implements Runnable {

    protected static long startToken;

    //TODO :: setup a Connection with Mycheker and store the TOKEN in a Variable

    //TODO :: Provide the Connectivity to others
    public ICUService() {
    }

    @Override
    public void run() {
        while (true){
            try {
                //System.out.println("ICU Server runing ...");
                //TODO :: check the Validation of th Token
            if (startToken!=0){
                long curentime=Instant.now().getEpochSecond();
                long calcStatToken = startToken+3500;
                if (calcStatToken<curentime) {
                    System.out.println(curentime+"\n" +calcStatToken);
                    handleToken();
                }
            }
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected String fetchJsonResponse(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 1. Set the Request Method
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // 2. Set Content-Type for x-www-form-urlencoded
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // 3. Prepare the Body Parameters
        StringJoiner sj = new StringJoiner("&");
        sj.add("grant_type=password");
        sj.add("username="+ Main.username);
        sj.add("password="+Main.password);
        byte[] postData = sj.toString().getBytes(StandardCharsets.UTF_8);

        // 4. Write the Data to the Connection
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        // 5. Read the Response
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            }
        } else {
            throw new RuntimeException("HTTP Error: " + responseCode);
        }
    }

    protected String fetchJsonResponseWithToken(String urlString,String method, String token, String jsonBody) {
        int responseCode;
        StringBuilder response = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = null;
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            if (jsonBody != null && !jsonBody.isEmpty()) {
                byte[] postData = jsonBody.getBytes(StandardCharsets.UTF_8);
                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postData);
                }
            }
            responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                    response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
            }
            else {
                // conn.getErrorStream() for errors from ICU
                throw new RuntimeException("HTTP Error: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response != null ? response.toString() : null;
    }


    protected void  handleToken(){
        //get a Token
        startToken = Instant.now().getEpochSecond();
        try {
            String json = fetchJsonResponse(Main.api+"/Token");

            JSONObject jo = new JSONObject(json);
            //System.out.printf("JSON Response: %s%n",jo);
            Main.token = jo.getString("access_token");
            System.out.println("Token :" +Main.token);
            //System.out.println("start Token: "+startToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
