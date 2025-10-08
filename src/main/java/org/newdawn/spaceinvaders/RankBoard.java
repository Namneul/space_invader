package org.newdawn.spaceinvaders;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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


        //배경 생성코드
        panel = new JPanel(){Image background = new ImageIcon(RankBoard.class.getResource("/ScoreboardBackground.png")).getImage();
            @Override
            public void paintComponent(Graphics g){
             super.paintComponent(g);
             g.drawImage(background, 0, 0, null);
             int starty = 100;

             g.setColor(Color.WHITE);
             int startXRank = 320;
             int startXName = 390;
             int startXScore = 460;
             for(int i = 0; i < data.size(); i++) {

                 g.drawString((i+1)+"위", startXRank, starty + i * 30);
                 g.drawString(data.get(i)[0], startXName, starty + i * 30);
                 g.drawString(data.get(i)[1], startXScore, starty + i * 30);
                }
            }
        };

        panel.setLayout(null);
        panel.setBounds(0, 0, 800, 600);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
     }


}
