package test;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.repository.AGServer;

import junit.framework.Assert;

import org.junit.Test;

public class AGServerTests extends AGAbstractTest {

    void verifyUserAndPassword(AGServer server, boolean anonymous) {
        String gotUser = server.getUser();
        String gotPassword = server.getPassword();

        String expectedUser = anonymous ? null : username();
        String expectedPassword = anonymous ? null : password();

        Assert.assertEquals(String.format("Expected user '%s' but got '%s'", expectedUser, gotUser),
                expectedUser, gotUser);
        Assert.assertEquals(String.format("Expected password '%s' but got '%s'", expectedPassword, gotPassword),
                expectedPassword, gotPassword);
    }

    @Test
    public void AGServerConstructorTest1() {
        // Test the AGServer(serverURL, username, password) constructor

        try (AGServer server = new AGServer(findServerUrl(), username(), password())) {
            verifyUserAndPassword(server, false);
        }

    }

    @Test
    public void AGServerConstructorTest2() {
        // Test the AGServer(username, password, httpClient) constructor 

        AGHTTPClient cli = new AGHTTPClient(findServerUrl()); 

        try (AGServer server = new AGServer(username(), password(), cli)) {
            verifyUserAndPassword(server, false);
        }

    }

    @Test
    public void AGServerConstructorTest3() {
        // Test the AGServer(httpClient) constructor 

        AGHTTPClient cli = new AGHTTPClient(findServerUrl());
        cli.setUsernameAndPassword(username(), password());

        try (AGServer server = new AGServer(cli)) {
            verifyUserAndPassword(server, false);
        }

    }

    @Test
    public void AGServerConstructorTest4() {
        // Test the AGServer(serverURL) constructor 

        try (AGServer server = new AGServer(findServerUrl())) {
            verifyUserAndPassword(server, true);
        }

    }

    
}
