package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.RankData;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class RankBoard {
    JFrame frame;
    JPanel panel;
     public RankBoard(ArrayList<RankData> ranking) throws SQLException {
         frame = new JFrame("Rank Board");
         frame.setPreferredSize(new Dimension(800,600));
         frame.setLayout(null);

         String[][] scores = new String[ranking.size()][2];
         for(int i = 0; i < ranking.size(); i++) {
             RankData rank = ranking.get(i);
             scores[i][0] = rank.getUsername();
             scores[i][1] = String.valueOf(rank.getScore());
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
