package org.newdawn.spaceinvaders.multiplay;
import java.io.Serializable;
import java.sql.*;
public class Login {
    private final String dbUrl = "jdbc:mysql://34.47.73.59:3306/spaceinvader";
    private final String dbUser = "remoteuser";
    private final String dbPass = "sshs8458";
    Connection con = null;
    PreparedStatement psmt = null;

    public Login() {
        //jdbc  드라이버에 연결
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch(ClassNotFoundException e){
            System.out.println("Driver not found" + e.getMessage());
        }
        //db 테이블을 connection객체랑 연결해주기
        try{
            con = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            System.out.println("successfully connected to the database");
        }catch(SQLException e){
            System.out.println("Failed to connect to the database"+ e.getMessage());
        }


    }

    public boolean signUp(String username, String password) {
        String sql = "insert into UserInfo values(?,?) ";
        try{
            psmt = con.prepareStatement(sql);
            psmt.setString(1,username);
            psmt.setString(2,password);
            psmt.executeUpdate();
            System.out.println("successfully inserted user");
            return true;
        }catch(SQLException e){
            System.out.println("Failed to insert into UserInfor values"+ e.getMessage());
            return false;
        }
    }

    public boolean login(String username, String password) {
        String sql = "select * from UserInfo where UserId=? and Password=?";
        try{
            psmt = con.prepareStatement(sql);
            psmt.setString(1,username);
            psmt.setString(2,password);
            ResultSet rs = psmt.executeQuery();
            if(rs.next()){
                System.out.println("successful login");
                psmt.close();
                return true;
            }
            System.out.println("failed login");
            return false;
        } catch (SQLException e) {
            System.out.println("Fail to login"+ e.getMessage());
            return false;
        }
    }

    public void deleteUser(String username) {
        String sql = "delete from UserInfo where UserId=?";
        try{
            psmt = con.prepareStatement(sql);
            psmt.setString(1, username);
            psmt.executeUpdate();
            System.out.println("successfully deleted user");
        } catch (SQLException e) {
            System.out.println("Failed to delete user"+ e.getMessage());
        }
    }

    public int getScore(String username) {
        String sql = "select Score from ScoreBoard where UserId=?";
        try{
            psmt = con.prepareStatement(sql);
            psmt.setString(1,username);
            psmt.executeUpdate();
            ResultSet rs = psmt.getResultSet();
            rs.next();
            System.out.println("getScore successf");
            return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Failed to get score "+ e.getMessage());
            return 1;
        }
    }

    public void insertScore(String username, int score) {
        String sql = "insert into ScoreBoard values(?,?)";
        try{
            psmt = con.prepareStatement(sql);
            psmt.setString(1,username);
            psmt.setInt(2,score);
            psmt.executeUpdate();
            System.out.println("successfully inserted score");
        }catch(SQLException e){
            System.out.println("Failed to insert score"+ e.getMessage());
        }
    }

    public ResultSet getAllScore() {
        String sql = "select * from ScoreBoard ORDER BY Score DESC";
        try{
            psmt = con.prepareStatement(sql);
            ResultSet rs = psmt.executeQuery();
            return rs;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

