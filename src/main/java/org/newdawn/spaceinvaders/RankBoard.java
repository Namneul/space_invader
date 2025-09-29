package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class RankBoard {
    JFrame frame;
    JPanel panel;
    ResultSet rs;
     public RankBoard(ResultSet rs) throws SQLException {
         this.rs = rs;
         frame = new JFrame("Rank Board");
         frame.setPreferredSize(new Dimension(800,600));
         frame.setLayout(null);

         ArrayList<String[]> data = new ArrayList<>();
         while(rs.next()) {
             String name = rs.getString(1);
             int score = rs.getInt(2);
             data.add(new String[]{name, String.valueOf(score)});
         }

         String[][] scores = new String[data.size()][2];
         for(int i = 0; i < data.size(); i++) {
             scores[i] = data.get(i);
         }

         String[] header = {"Name", "Score"};

        JTable table = new JTable(scores, header);
        table.setFont(new Font("Serif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Serif", Font.PLAIN, 20));
        table.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(0, 0, 800, 600);

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, 800, 600);
        panel.add(scrollPane);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
     }
}
