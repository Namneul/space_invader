package org.newdawn.spaceinvaders.client;

import org.newdawn.spaceinvaders.client.multiplay.RankData;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class RankBoard {
    JFrame frame;
    JPanel panel;
     public RankBoard(ArrayList<RankData> ranking){
         frame = new JFrame("Rank Board");
         frame.setPreferredSize(new Dimension(800,600));
         frame.setLayout(null);

         String[][] scores = new String[ranking.size()][2];
         for(int i = 0; i < 14; i++) {
             RankData rank = ranking.get(i);
             scores[i][0] = rank.getUsername();
             scores[i][1] = String.valueOf(rank.getScore());
         }

         String[] header = {"Name", "Score"};

        panel = new JPanel(){Image background = new ImageIcon(RankBoard.class.getResource("/ScoreboardBackground.png")).getImage();

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(background,0,0,null);
                int starty = 100;

                g.setColor(Color.WHITE);
                int startXRank = 320;
                int startXName = 390;
                int startXScore = 460;
                for (int i = 0; i < 14; i++) {
                    g.drawString((i+1)+"위", startXRank, starty + i * 30);
                    g.drawString(scores[i][0], startXName, starty + i * 30);
                    g.drawString(scores[i][1], startXScore, starty + i * 30);
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
