package org.newdawn.spaceinvaders.multiplay;

import java.sql.*;
import java.util.ArrayList;

public class Login {
    private final String dbUrl = "jdbc:mysql://34.47.73.59:3306/spaceinvader";
    private final String dbUser = "remoteuser";
    private final String dbPass = "sshs8458";
    private Connection con = null;
    // private PreparedStatement psmt = null; // ★★★ 공유해서 쓰던 이 변수를 삭제!

    public Login() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            System.out.println("successfully connected to the database");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            // 여기서 연결 실패 시 con이 null이 될 수 있으므로, 각 메소드에서 null 체크를 하는 것이 좋습니다.
        }
    }

    // 각 메소드가 자기만의 PreparedStatement를 만들고 자동으로 닫도록 수정
    public boolean signUp(String username, String password) {
        String sql = "INSERT INTO UserInfo (UserId, Password) VALUES(?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) { // try-with-resources
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to insert user: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String username, String password) {
        String sql = "SELECT * FROM UserInfo WHERE UserId=? AND Password=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) { // try-with-resources
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // 결과가 있으면 true, 없으면 false
            }
        } catch (SQLException e) {
            System.out.println("Fail to login: " + e.getMessage());
            return false;
        }
    }

    public void insertScore(String username, int score) {
        // 이 메소드는 다른 메소드들을 호출하므로 그대로 둬도 안전합니다.
        try {
            ensureUserExists(username);
            String sql = "INSERT INTO ScoreBoard (UserId, Score) VALUES (?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, score);
                ps.executeUpdate();
                System.out.println("Successfully inserted score for " + username);
            }
        } catch (SQLException e) {
            System.out.println("Failed to insert score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureUserExists(String userId) throws SQLException {
        // ON DUPLICATE KEY UPDATE는 이미 원자적이므로 안전합니다.
        String sql = "INSERT INTO UserInfo (UserId, Password) VALUES (?, '') ON DUPLICATE KEY UPDATE UserId = UserId";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public ArrayList<RankData> getAllScore() {
        ArrayList<RankData> ranking = new ArrayList<>();
        String sql = "SELECT * FROM ScoreBoard ORDER BY Score DESC";
        try (PreparedStatement ps = con.prepareStatement(sql); // try-with-resources
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("UserId");
                int score = rs.getInt("Score");
                ranking.add(new RankData(username, score));
            }
        } catch (SQLException e) {
            System.out.println("Failed to get all scores: " + e.getMessage());
            // 랭킹 조회 실패 시 비어있는 리스트를 반환하는 것이 더 안전합니다.
        }
        return ranking;
    }

    // 사용하지 않는 메소드들은 일단 그대로 둡니다.
    public void deleteUser(String username) { /* ... */ }
    public int getScore(String username) { /* ... */ return 0; }
}