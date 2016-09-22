package org.jivesoftware.openfire.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.msn.noSqlConf.NoSqlConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**

 * RedisAuthProvider的实现。
 * 通过服务器的Redis数据库缓存MySQL数据库的ofUser表。
 * 用户登录的时候先查询Redis数据库，如果存在则通过Redis认证。如果用户不存在则查询MySQL，
 * 同时将信息更新至Redis中。
 * 
 * 固定使用redis 1号数据库
 * 用户数据结构为hash表
 * key为user:${username}
 *
 * @author 赵凯
 */
public class RedisAuthProvider implements AuthProvider {

	private static final Logger Log = LoggerFactory.getLogger(RedisAuthProvider.class);

    private static final String LOAD_PASSWORD =
            "SELECT plainPassword,encryptedPassword FROM ofUser WHERE username=?";
    private static final String UPDATE_PASSWORD =
            "UPDATE ofUser SET plainPassword=?, encryptedPassword=? WHERE username=?";
    
    private Jedis redis;

    /**
     * Constructs a new DefaultAuthProvider.
     */
    public RedisAuthProvider() {
 		redis=NoSqlConf.createRedis();
    }

    public void authenticate(String username, String password) throws UnauthorizedException {
        if (username == null || password == null) {
            throw new UnauthorizedException();
        }
        username = username.trim().toLowerCase();
        if (username.contains("@")) {
            // Check that the specified domain matches the server's domain
            int index = username.indexOf("@");
            String domain = username.substring(index + 1);
            if (domain.equals(XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {
                username = username.substring(0, index);
            } else {
                // Unknown domain. Return authentication failed.
                throw new UnauthorizedException();
            }
        }
        try {
            if (!password.equals(getPassword(username))) {
                throw new UnauthorizedException();
            }
        }
        catch (UserNotFoundException unfe) {
            throw new UnauthorizedException();
        }
        
        
        // Got this far, so the user must be authorized.
    }

    public void authenticate(String username, String token, String digest) throws UnauthorizedException {
        if (username == null || token == null || digest == null) {
            throw new UnauthorizedException();
        }
        username = username.trim().toLowerCase();
        if (username.contains("@")) {
            // Check that the specified domain matches the server's domain
            int index = username.indexOf("@");
            String domain = username.substring(index + 1);
            if (domain.equals(XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {
                username = username.substring(0, index);
            } else {
                // Unknown domain. Return authentication failed.
                throw new UnauthorizedException();
            }
        }
        try {
            String password = getPassword(username);
            String anticipatedDigest = AuthFactory.createDigest(token, password);
            if (!digest.equalsIgnoreCase(anticipatedDigest)) {
                throw new UnauthorizedException();
            }
        }
        catch (UserNotFoundException unfe) {
            throw new UnauthorizedException();
        }
        // Got this far, so the user must be authorized.
    }

    public boolean isPlainSupported() {
        return true;
    }

    public boolean isDigestSupported() {
        return true;
    }

    public String getPassword(String username) throws UserNotFoundException {
        if (!supportsPasswordRetrieval()) {
            // Reject the operation since the provider is read-only
            throw new UnsupportedOperationException();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        if (username.contains("@")) {
            // Check that the specified domain matches the server's domain
            int index = username.indexOf("@");
            String domain = username.substring(index + 1);
            if (domain.equals(XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {
                username = username.substring(0, index);
            } else {
                // Unknown domain.
                throw new UserNotFoundException();
            }
        }
        
        try{
        	String redisEncryptedPassword=redis.hget("user:"+username, "encryptedPassword");
        	String redisPlainPassword=redis.hget("user:"+username, "plainPassword");
        
        	if(redisEncryptedPassword!=null)//the user is saved in Redis
        	{
        		System.out.println("从Redis中获取"+username+"的加密密码。");
        		return AuthFactory.decryptPassword(redisEncryptedPassword);
        	}
        	else if(redisEncryptedPassword==null&&redisPlainPassword!=null)
        	{
        		System.out.println("从Redis中获取"+username+"的普通密码。");
        		return redisPlainPassword;
        	}
        }
        catch(JedisConnectionException e)
        {
        	System.err.println("RedisAuthProvider::无法连接Redis服务器！");
        }
        
        
        //the user is not saved in Redis
        try {
        	con = DbConnectionManager.getConnection();
        	pstmt = con.prepareStatement(LOAD_PASSWORD);
        	pstmt.setString(1, username);
        	rs = pstmt.executeQuery();
        	if (!rs.next()) {
        		throw new UserNotFoundException(username);
        	}
        	String plainText = rs.getString(1);
        	String encrypted = rs.getString(2);
        	System.out.println("从MySQL中获取"+username+"的密码。");
        	
        	try
    		{
        		if(plainText!=null)
        			redis.hset("user:"+username, "plainPassword", plainText);
        		
        		if(encrypted!=null)
        			redis.hset("user:"+username, "encryptedPassword", encrypted);
        		
    			System.out.println("将"+username+"的密码成功保存在Redis中。");
    		}
    		catch(JedisConnectionException e)
    		{
    			System.err.println("Warning:无法连接Redis服务器！");
    		}
        	
        	if (encrypted != null) {
        		try {
        			return AuthFactory.decryptPassword(encrypted);
        		}
        		catch (UnsupportedOperationException uoe) {
        			// Ignore and return plain password instead.
        		}
        	}
        	return plainText;
        }
        catch (SQLException sqle) {
        	throw new UserNotFoundException(sqle);
        }
        finally {
        	DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        
    }

    public void setPassword(String username, String password) throws UserNotFoundException {
        // Determine if the password should be stored as plain text or encrypted.
        boolean usePlainPassword = JiveGlobals.getBooleanProperty("user.usePlainPassword");
        String encryptedPassword = null;
        if (username.contains("@")) {
            // Check that the specified domain matches the server's domain
            int index = username.indexOf("@");
            String domain = username.substring(index + 1);
            if (domain.equals(XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {
                username = username.substring(0, index);
            } else {
                // Unknown domain.
                throw new UserNotFoundException();
            }
        }
        if (!usePlainPassword) {
            try {
                encryptedPassword = AuthFactory.encryptPassword(password);
                // Set password to null so that it's inserted that way.
                password = null;
            }
            catch (UnsupportedOperationException uoe) {
                // Encryption may fail. In that case, ignore the error and
                // the plain password will be stored.
            }
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_PASSWORD);
            if (password == null) {
                pstmt.setNull(1, Types.VARCHAR);
            }
            else {
                pstmt.setString(1, password);
            }
            if (encryptedPassword == null) {
                pstmt.setNull(2, Types.VARCHAR);
            }
            else {
                pstmt.setString(2, encryptedPassword);
            }
            pstmt.setString(3, username);
            pstmt.executeUpdate();
            
            try
            {
            	if(encryptedPassword!=null)
            		redis.hset("user:"+username, "encryptedPassword", encryptedPassword);
            	if(password!=null)
            		redis.hset("user:"+username, "plainPassword", password);
            	
            	System.out.println("成功修改Redis中"+username+"的普通密码和加密密码。");
            }
            catch(JedisConnectionException e)
            {
            	System.err.println("RedisAuthProvider::无法连接Redis服务器！");
            }
        }
        catch (SQLException sqle) {
            throw new UserNotFoundException(sqle);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    public boolean supportsPasswordRetrieval() {
        return true;
    }
}
