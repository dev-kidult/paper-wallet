package eth.devkidult.paperwallet.Component;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class Dispatcher {

    HttpURLConnection connection;

    public void disconnect(){
        if(connection!=null) {
            connection.disconnect();
        }
    }

    public String response() throws IOException {
        InputStreamReader is = new InputStreamReader(connection.getInputStream());
        BufferedReader rd = new BufferedReader(is);
        String response = rd.readLine();
        rd.close();
        is.close();
        return response;
    }

    public void setConnection(String url) throws IOException{
        disconnect();
        HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-Requested-With", "Curl");
        connection.setDoOutput(true);
        this.connection = connection;
    }

    public void getTokens(String address) throws IOException {
        String url = "https://api.ethplorer.io/getAddressInfo/"+address+"?apiKey=freekey";
        setConnection(url);
    }

    public void getTokenInfo(String address) throws IOException{
        String url = "https://api.ethplorer.io/getTokenInfo/"+address+"?apiKey=freekey";
        setConnection(url);
    }
}
