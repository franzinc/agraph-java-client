/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGServer;
import junit.framework.Assert;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;


import java.util.List;

public class UserManagementTests extends AGAbstractTest {

    @Test
    public void addDeleteUsers() throws Exception {
        String newuser1 = "newuser1";
        String newuser2 = "newuser2";
        List<String> users = server.listUsers();
        Assert.assertTrue("expected some users", users.size() > 0);
        if (!users.contains(newuser1)) {
            server.addUser(newuser1, "password1");
        }
        if (!users.contains(newuser2)) {
            server.addUser(newuser2, "password2");
        }
        users = server.listUsers();
        Assert.assertTrue(users.contains(newuser1));
        Assert.assertTrue(users.contains(newuser2));
        server.deleteUser(newuser2);
        users = server.listUsers();
        Assert.assertTrue(users.contains(newuser1));
        Assert.assertFalse(users.contains(newuser2));


        // Verify the server is responsive using newuser1's existing auth.
        AGServer testServer1 = new AGServer(server.getServerURL(), newuser1, "password1");

        // give newuser1 'session' permission, so it can be checked for.
        server.addUserPermission(newuser1, "session");

        // make a request using newuser1, verify the permission we added is visible
        List<String> permissions = testServer1.listUserPermissions(newuser1);
        Assert.assertTrue(permissions.contains("session"));

        // change newuser1 password
        server.changeUserPassword(newuser1, "newpassword1");

        // TODO: assertThrows() exists in junit5+
        try { 
            permissions = testServer1.listUserPermissions(newuser1);
            Assert.fail("Expected AGHttpException exception not thrown");
        } catch (AGHttpException c) {
            if (c.getCause() instanceof UnauthorizedException) {
                // success
            } else {
                Assert.fail("Expected UnauthorizedException exception not thrown");
            }
        }

        // this should work
        AGServer testServer2 = new AGServer(server.getServerURL(), newuser1, "newpassword1");
        permissions = testServer2.listUserPermissions(newuser1);
        Assert.assertEquals(1, permissions.size());
        Assert.assertTrue(permissions.contains("session"));

        // delete newuser1
        server.deleteUser("newuser1");
        users = server.listUsers();
        Assert.assertFalse(users.contains("newuser1"));
    }

    @Test
    public void userAccess() throws Exception {
        List<String> users = server.listUsers();
        if (!users.contains("newuser1")) {
            server.addUser("newuser1", "newuser1");
        }
        if (!users.contains("newuser2")) {
            server.addUser("newuser2", "newuser2");
        }
        server.addUserAccess("newuser1", true, false, "*", null);
        server.addUserAccess("newuser2", true, true, repo.getCatalog().getCatalogName(), repo.getRepositoryID());
        JSONArray accessList = server.listUserAccess("newuser1");
        JSONObject access = accessList.getJSONObject(0);
        Assert.assertTrue(access.getBoolean("read"));
        Assert.assertFalse(access.getBoolean("write"));
        Assert.assertEquals(access.getString("catalog"), "*");
        Assert.assertEquals(access.getString("repository"), "*");
        accessList = server.listUserAccess("newuser2");
        access = accessList.getJSONObject(0);
        Assert.assertTrue(access.getBoolean("read"));
        Assert.assertTrue(access.getBoolean("write"));
        Assert.assertEquals(access.getString("catalog"), repo.getCatalog().getCatalogName());
        Assert.assertEquals(access.getString("repository"), repo.getRepositoryID());
        server.deleteUser("newuser1");
        server.deleteUser("newuser2");
    }

    @Test
    public void userRoles() throws Exception {
        final String user = "user-test";
        final String role = "role-test";
        final String role2delete = "role-to-delete";

        server.addUser(user, "xyzzy");
        Assert.assertTrue(server.listUsers().contains(user));

        server.addUserAccess(user, true, true, null, null);
        JSONArray accessList = server.listUserEffectiveAccess(user);
        Assert.assertEquals(1, accessList.length());
        JSONObject access = accessList.getJSONObject(0);
        Assert.assertTrue(access.getBoolean("read"));
        Assert.assertTrue(access.getBoolean("write"));
        Assert.assertEquals(access.getString("catalog"), "*");
        Assert.assertEquals(access.getString("repository"), "*");

       /* beginning in 7.4.0 users are given session permission
        * remove so that tests continue to work
        */
       server.deleteUserPermission(user, "session");

        List<String> permissions = server.listUserPermissions(user);

        Assert.assertTrue(permissions.isEmpty());

        permissions = server.listUserEffectivePermissions(user);
        Assert.assertTrue(permissions.isEmpty());

        server.addUserPermission(user, "eval");
        server.addUserPermission(user, "replication");
        server.deleteUserPermission(user, "replication");

        permissions = server.listUserPermissions(user);
        Assert.assertEquals(1, permissions.size());
        Assert.assertTrue(permissions.contains("eval"));

        List<String> roles = server.listRoles();
        Assert.assertEquals(0, roles.size());

        server.addRole(role);
        server.addRole(role2delete);
        roles = server.listRoles();
        Assert.assertEquals(2, roles.size());

        server.addRoleAccess(role2delete, true, true, "/", "*");
        accessList = server.listRoleAccess(role2delete);
        Assert.assertEquals(1, accessList.length());
        access = accessList.getJSONObject(0);
        Assert.assertTrue(access.getBoolean("read"));
        Assert.assertTrue(access.getBoolean("write"));
        Assert.assertEquals(access.getString("catalog"), "/");
        Assert.assertEquals(access.getString("repository"), "*");
        server.addRoleSecurityFilter(role2delete, "allow", "<http://allowed>", null, null, null);
        JSONArray filters = server.listRoleSecurityFilters(role2delete, "allow");
        JSONObject filter = filters.getJSONObject(0);
        Assert.assertEquals("<http://allowed>", filter.optString("s"));
        Assert.assertEquals("", filter.optString("p"));
        Assert.assertEquals("", filter.optString("o"));
        Assert.assertEquals("", filter.optString("g"));
        server.deleteRoleSecurityFilter(role2delete, "allow", "<http://allowed>", null, null, null);
        filters = server.listRoleSecurityFilters(role2delete, "allow");
        Assert.assertEquals(0, filters.length());
        server.addUserRole(user, role2delete);
        accessList = server.listUserEffectiveAccess(user);
        Assert.assertEquals(2, accessList.length());

        server.addUserSecurityFilter(user, "allow", "<http://allowed>", null, null, null);
        filters = server.listUserSecurityFilters(user, "allow");
        Assert.assertEquals(1, filters.length());
        filter = filters.getJSONObject(0);
        Assert.assertEquals("<http://allowed>", filter.optString("s"));
        Assert.assertEquals("", filter.optString("p"));
        Assert.assertEquals("", filter.optString("o"));
        Assert.assertEquals("", filter.optString("g"));

        server.deleteUserSecurityFilter(user, "allow", "<http://allowed>", null, null, null);
        server.deleteUserRole(user, role2delete);
        server.deleteRoleAccess(role2delete, true, true, "/", "*");
        accessList = server.listRoleAccess(role2delete);
        Assert.assertEquals(0, accessList.length());

        server.deleteRole(role2delete);
        roles = server.listRoles();
        Assert.assertEquals(1, roles.size());

        server.addRolePermission(role, "eval");
        server.addRolePermission(role, "session");
        server.deleteRolePermission(role, "eval");
        permissions = server.listRolePermissions(role);
        Assert.assertEquals(1, permissions.size());
        Assert.assertTrue(permissions.contains("session"));

        server.addUserRole(user, role);
        roles = server.listUserRoles(user);
        Assert.assertEquals(1, roles.size());
        Assert.assertTrue(roles.contains(role));

        permissions = server.listUserEffectivePermissions(user);
        Assert.assertEquals(2, permissions.size());
        Assert.assertTrue(permissions.contains("eval"));
        Assert.assertTrue(permissions.contains("session"));

        roles = server.listRoles();

        server.deleteUserRole(user, role);
        roles = server.listUserRoles(user);
        Assert.assertTrue(roles.isEmpty());

        server.deleteRole(role);
        roles = server.listRoles();
        Assert.assertTrue(roles.isEmpty());

        server.deleteUserAccess(user, true, true, null, null);
        accessList = server.listUserAccess(user);
        Assert.assertEquals(0, accessList.length());

        server.deleteUser(user);
        Assert.assertFalse(server.listUsers().contains(user));
    }
}
