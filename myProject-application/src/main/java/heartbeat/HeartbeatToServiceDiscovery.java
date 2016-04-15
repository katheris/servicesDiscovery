package heartbeat;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

@Startup
@Singleton
public class HeartbeatToServiceDiscovery {
    private ServiceRegistration registration;

    @PostConstruct
    public void setupServiceRegistration() {
        registration =
                        new ServiceRegistration();
        registration.registerService();
    }

    @Schedule(second = "*/20", hour = "*", minute = "*",
              persistent = false)
    public void heartbeat() {
        try {
        System.out.println("Processing heartbeat");
        registration.sendHeartbeatUrl();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public class ServiceRegistration {

        private String authorizationToken;
        private String endpointUrl;
        private String heartbeatUrl;

        public ServiceRegistration() {
            String vcapServices = System.getenv("VCAP_SERVICES");
            if (vcapServices != null) {
                parseVcapServices(vcapServices);
            } else {
                throw new IllegalArgumentException("No VCAP_SERVICES was supplied");
            }
        }
        
        private void parseVcapServices(String vcapServicesEnv) {
            JsonObject vcapServices = Json.createReader(new StringReader(vcapServicesEnv)).readObject();
            JsonArray cloudantObjectArray = vcapServices.getJsonArray("service_discovery");
            JsonObject cloudantObject = cloudantObjectArray.getJsonObject(0);
            JsonObject cloudantCredentials = cloudantObject.getJsonObject("credentials");
            JsonString cloudantUsername = cloudantCredentials.getJsonString("auth_token");
            authorizationToken = cloudantUsername.getString();
            JsonString cloudantUrl = cloudantCredentials.getJsonString("url");
            endpointUrl = cloudantUrl.getString();
        }

        public void registerService() {
            Client client = ClientBuilder.newClient();
            String url = endpointUrl + "/api/v1/instances";
            System.out.println("Testing " + url);
            Entity<String> entity = Entity.json("{\"service_name\":\"service1\",\"endpoint\": { \"type\":\"tcp\", \"value\": \"http://service1kate.mybluemix.net/\" }," +
                "\"status\":\"UP\", \"ttl\":30}");
            Response response = client.target(url).request().header("Authorization", "Bearer " + authorizationToken).post(entity);
            int responseStatus = response.getStatus();
            JsonObject responseObject = response.readEntity(JsonObject.class);
            System.out.println("registerService() response object is " + responseObject + " responseStatus:" + responseStatus);
            heartbeatUrl = responseObject.getJsonObject("links").getString("heartbeat");
        }

        public void sendHeartbeatUrl() throws MalformedURLException, IOException {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(heartbeatUrl).openConnection();
            urlConnection.setRequestProperty("Authorization", "Bearer " + authorizationToken);
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoOutput(true);
            urlConnection.setFixedLengthStreamingMode(0);
            int status = urlConnection.getResponseCode();
            System.out.println("Response status is " + status);
            if (status != 200) {
               System.out.println("Response is " + urlConnection.getResponseMessage());
            }
        }

        public String getAuthorizationToken() {
            return authorizationToken;
        }
    }
}
