/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/ 
package fvt;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class TestApplication {

    @Test
    public void testDeployment() {
        String context = "http://service1kate.mybluemix.net/myLibertyApp";
        String url = "http://" + context + "/index.html";
        System.out.println("Testing " + url);
        Response response = sendRequest(url, "GET");
        int responseCode = response.getStatus();
        assertTrue("Incorrect response code: " + responseCode,
                   responseCode == 200);
        String responseString = response.readEntity(String.class);
        response.close();
        assertTrue("Incorrect response, response is " + responseString, responseString.contains("<h1>Welcome to your Liberty Application</h1>"));
    }

    public Response sendRequest(String url, String requestType) {
        Client client = ClientBuilder.newClient();
        System.out.println("Testing " + url);
        WebTarget target = client.target(url);
        Invocation.Builder invoBuild = target.request();
        Response response = invoBuild.build(requestType).invoke();
        return response;
    }

}
